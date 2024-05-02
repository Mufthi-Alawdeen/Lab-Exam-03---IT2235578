package com.example.football

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import java.util.*

class Game : AppCompatActivity() {

    private lateinit var bunnyImage: ImageView
    private lateinit var carrotImage: ImageView
    private lateinit var bombImage: ImageView
    private var initialX = 0f
    private val random = Random()
    private var score = 0
    private var gameSpeedMultiplier = 1.0
    private var gamePaused = false
    private lateinit var dropRunnable: Runnable
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        bunnyImage = findViewById(R.id.rabbitImage)
        carrotImage = findViewById(R.id.carrotImage)
        bombImage = findViewById(R.id.bombImage)

        carrotImage.visibility = View.INVISIBLE
        bombImage.visibility = View.INVISIBLE

        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val savedGameSpeed = sharedPreferences.getString("gameSpeed", "1X")
        gameSpeedMultiplier = when (savedGameSpeed) {
            "1X" -> 1.0
            "2X" -> 2.0
            else -> 1.0
        }

        gamePaused = sharedPreferences.getBoolean("isGamePaused", false)

        bunnyImage.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bunnyImage.x - event.rawX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX + initialX
                    if (newX >= 0 && newX <= (bunnyImage.parent as View).width - bunnyImage.width) {
                        bunnyImage.x = newX
                    }
                    true
                }
                else -> false
            }
        }

        val pauseButton: Button = findViewById(R.id.pauseButton)
        pauseButton.setOnClickListener {
            if (gamePaused) {
                resumeGame()
                pauseButton.text = "Pause"
            } else {
                pauseGame()
                pauseButton.text = "Resume"
            }
        }

        startRandomDropping()
    }

    private fun pauseGame() {
        showPauseDialog()
        gamePaused = true
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("isGamePaused", true)
        editor.apply()
    }

    private fun resumeGame() {
        dismissPauseDialog()
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("isGamePaused")
        editor.apply()
        gamePaused = false
        startRandomDropping()
    }

    private fun startRandomDropping() {
        dropRunnable = object : Runnable {
            override fun run() {
                if (!gamePaused) {
                    // Drop multiple objects in each iteration
                    val numberOfDrops = 3 // Adjust the number of drops as needed
                    repeat(numberOfDrops) {
                        val dropCarrot = random.nextBoolean()
                        if (dropCarrot) {
                            dropCarrotFromTop()
                        } else {
                            dropBombFromTop()
                        }
                    }

                    val nextDelay = (random.nextInt(3000) + 1000) / gameSpeedMultiplier.toInt()
                    handler.postDelayed(this, nextDelay.toLong())
                }
            }
        }

        handler.post(dropRunnable)
    }


    private fun dropCarrotFromTop() {
        val newCarrot = ImageView(this)
        newCarrot.setImageResource(R.drawable.carrot)
        newCarrot.layoutParams = carrotImage.layoutParams

        (findViewById<View>(android.R.id.content) as? ViewGroup)?.addView(newCarrot)

        val screenWidth = (findViewById<View>(android.R.id.content) as ViewGroup).width
        val randomX = random.nextInt(screenWidth - newCarrot.width)

        newCarrot.translationX = randomX.toFloat()
        newCarrot.translationY = -newCarrot.height.toFloat()

        val carrotAnimator = ObjectAnimator.ofFloat(
            newCarrot,
            "translationY",
            0f,
            (findViewById<View>(android.R.id.content) as ViewGroup).height.toFloat()
        )
        carrotAnimator.apply {
            duration = (1500 / gameSpeedMultiplier).toLong()
            interpolator = AccelerateInterpolator()
            start()
        }

        carrotAnimator.doOnStart {
            newCarrot.visibility = View.VISIBLE
        }

        carrotAnimator.doOnEnd {
            (findViewById<View>(android.R.id.content) as? ViewGroup)?.removeView(newCarrot)
        }

        carrotAnimator.doOnEnd {
            if (isBunnyCatchingCarrot(newCarrot)) {
                score++
                updateScore()
            }
        }

        // Check for collision with the rabbit image
        carrotAnimator.addUpdateListener {
            if (isCollision(newCarrot, bunnyImage)) {
                (findViewById<View>(android.R.id.content) as? ViewGroup)?.removeView(newCarrot)
                // Handle collision action here, like decreasing score or ending the game
            }
        }
    }

    private fun dropBombFromTop() {
        val newBomb = ImageView(this)
        newBomb.setImageResource(R.drawable.spike)
        newBomb.layoutParams = bombImage.layoutParams

        (findViewById<View>(android.R.id.content) as? ViewGroup)?.addView(newBomb)

        val screenWidth = (findViewById<View>(android.R.id.content) as ViewGroup).width
        val randomX = random.nextInt(screenWidth - newBomb.width)

        newBomb.translationX = randomX.toFloat()
        newBomb.translationY = -newBomb.height.toFloat()

        val shotAnimator = ObjectAnimator.ofFloat(
            newBomb,
            "translationY",
            0f,
            (findViewById<View>(android.R.id.content) as ViewGroup).height.toFloat()
        )
        shotAnimator.apply {
            duration = (1500 / gameSpeedMultiplier).toLong()
            interpolator = AccelerateInterpolator()
            start()
        }

        shotAnimator.doOnStart {
            newBomb.visibility = View.VISIBLE
        }

        shotAnimator.doOnEnd {
            (findViewById<View>(android.R.id.content) as? ViewGroup)?.removeView(newBomb)
        }

        shotAnimator.doOnEnd {
            if (isBunnyCatchingCarrot(newBomb)) {
                gameOver()
            }
        }

        // Check for collision with the rabbit image
        shotAnimator.addUpdateListener {
            if (isCollision(newBomb, bunnyImage)) {
                (findViewById<View>(android.R.id.content) as? ViewGroup)?.removeView(newBomb)
                // Handle collision action here, like decreasing score or ending the game
            }
        }
    }

    private fun isBunnyCatchingCarrot(carrot: ImageView): Boolean {
        val carrotX = bunnyImage.x
        val carrotWidth = bunnyImage.width
        val ballX = carrot.translationX
        return ballX in carrotX..(carrotX + carrotWidth)
    }

    private fun gameOver() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Game Over")
            .setMessage("Your Score: $score")
            .setPositiveButton("Retry") { _, _ ->
                resetGame()
            }
            .setNegativeButton("Go Home") { _, _ ->
                val intent = Intent(this, LaunchPage1::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }

        val dialog = builder.create()
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            val retryIcon = resources.getDrawable(R.drawable.restart)
            retryIcon.setBounds(0, 0, 60, 60)
            positiveButton.setCompoundDrawables(retryIcon, null, null, null)

            val goHomeIcon = resources.getDrawable(R.drawable.exit)
            goHomeIcon.setBounds(0, 0, 60, 60)
            negativeButton.setCompoundDrawables(goHomeIcon, null, null, null)
        }

        dialog.show()
    }

    private fun resetGame() {
        score = 0
        updateScore()
        startRandomDropping()
    }

    private fun updateScore() {
        val scoreTextView = findViewById<TextView>(R.id.scoreTextView)
        scoreTextView.text = "Score: $score"

        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val highestScore = sharedPreferences.getInt("highestScore", 0)
        if (score > highestScore) {
            val editor = sharedPreferences.edit()
            editor.putInt("highestScore", score)
            editor.apply()
        }
    }

    private var pauseDialog: AlertDialog? = null

    private fun showPauseDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Game Paused")
            .setMessage("Tap 'Resume' to continue playing.")
            .setPositiveButton("Resume") { _, _ -> resumeGame() }
            .setNegativeButton("Go Home") { _, _ ->
                val intent = Intent(this, LaunchPage1::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
        pauseDialog = builder.create()
        pauseDialog?.setCancelable(false)
        pauseDialog?.show()
    }

    private fun dismissPauseDialog() {
        pauseDialog?.dismiss()
    }

    private fun isCollision(view1: View, view2: View): Boolean {
        val location1 = IntArray(2)
        val location2 = IntArray(2)
        view1.getLocationOnScreen(location1)
        view2.getLocationOnScreen(location2)

        val rect1 = Rect(
            location1[0], location1[1], location1[0] + view1.width, location1[1] + view1.height
        )
        val rect2 = Rect(
            location2[0], location2[1], location2[0] + view2.width, location2[1] + view2.height
        )

        return rect1.intersect(rect2)
    }
}
