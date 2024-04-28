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
                    val dropBall = random.nextBoolean()
                    if (dropBall) {
                        dropBallFromTop()
                    } else {
                        dropShotFromTop()
                    }

                    val nextDelay = (random.nextInt(3000) + 1000) / gameSpeedMultiplier.toInt()
                    handler.postDelayed(this, nextDelay.toLong())
                }
            }
        }

        handler.post(dropRunnable)
    }

    private fun dropBallFromTop() {
        val newBall = ImageView(this)
        newBall.setImageResource(R.drawable.carrot)
        newBall.layoutParams = carrotImage.layoutParams

        (findViewById<View>(android.R.id.content) as? ViewGroup)?.addView(newBall)

        val screenWidth = (findViewById<View>(android.R.id.content) as ViewGroup).width
        val randomX = random.nextInt(screenWidth - newBall.width)

        newBall.translationX = randomX.toFloat()
        newBall.translationY = -newBall.height.toFloat()

        val ballAnimator = ObjectAnimator.ofFloat(
            newBall,
            "translationY",
            0f,
            (findViewById<View>(android.R.id.content) as ViewGroup).height.toFloat()
        )
        ballAnimator.apply {
            duration = (1500 / gameSpeedMultiplier).toLong()
            interpolator = AccelerateInterpolator()
            start()
        }

        ballAnimator.doOnStart {
            newBall.visibility = View.VISIBLE
        }

        ballAnimator.doOnEnd {
            (findViewById<View>(android.R.id.content) as? ViewGroup)?.removeView(newBall)
        }

        ballAnimator.doOnEnd {
            if (isGoalieCatchingBall(newBall)) {
                score++
                updateScore()
            }
        }

        // Check for collision with the rabbit image
        ballAnimator.addUpdateListener {
            if (isCollision(newBall, bunnyImage)) {
                (findViewById<View>(android.R.id.content) as? ViewGroup)?.removeView(newBall)
                // Handle collision action here, like decreasing score or ending the game
            }
        }
    }

    private fun dropShotFromTop() {
        val newShot = ImageView(this)
        newShot.setImageResource(R.drawable.spike)
        newShot.layoutParams = bombImage.layoutParams

        (findViewById<View>(android.R.id.content) as? ViewGroup)?.addView(newShot)

        val screenWidth = (findViewById<View>(android.R.id.content) as ViewGroup).width
        val randomX = random.nextInt(screenWidth - newShot.width)

        newShot.translationX = randomX.toFloat()
        newShot.translationY = -newShot.height.toFloat()

        val shotAnimator = ObjectAnimator.ofFloat(
            newShot,
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
            newShot.visibility = View.VISIBLE
        }

        shotAnimator.doOnEnd {
            (findViewById<View>(android.R.id.content) as? ViewGroup)?.removeView(newShot)
        }

        shotAnimator.doOnEnd {
            if (isGoalieCatchingBall(newShot)) {
                gameOver()
            }
        }

        // Check for collision with the rabbit image
        shotAnimator.addUpdateListener {
            if (isCollision(newShot, bunnyImage)) {
                (findViewById<View>(android.R.id.content) as? ViewGroup)?.removeView(newShot)
                // Handle collision action here, like decreasing score or ending the game
            }
        }
    }

    private fun isGoalieCatchingBall(ball: ImageView): Boolean {
        val goalieX = bunnyImage.x
        val goalieWidth = bunnyImage.width
        val ballX = ball.translationX
        return ballX in goalieX..(goalieX + goalieWidth)
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
