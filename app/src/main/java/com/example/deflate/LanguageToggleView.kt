package com.example.deflate

import android.content.Context
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.TouchDelegate
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.TextView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator


class LanguageToggleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val thumb: View
    private val thumbText: TextView
    private val rightLabel: TextView

    // dp sizes -> px
    private val density = resources.displayMetrics.density
    private val trackDp = 142f
    private val thumbDp = 76f
    private val trackWidthPx = trackDp * density
    private val thumbWidthPx = thumbDp * density
    private val thumbTravelPx = trackWidthPx - thumbWidthPx // 66 dp? but compute exact in px

    private val animDuration = 180L
    private val interpolator = FastOutSlowInInterpolator()

    // true = left (English) selected; false = right (Afrikaans) selected
    private var _isLeftSelected = true
    var isLeftSelected: Boolean
        get() = _isLeftSelected
        set(value) {
            setLeftSelected(value, animate = false)
        }

    private var listener: ((Boolean) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_language_toggle, this, true)

        thumb = findViewById(R.id.language_thumb)
        thumbText = findViewById(R.id.language_thumb_text)
        rightLabel = findViewById(R.id.language_right_label)

        // default state
        setLeftSelected(_isLeftSelected, animate = false)

        // accessible & clickable
        isClickable = true
        isFocusable = true
        contentDescription = "Language toggle"

        setOnClickListener {
            // Toggle selection on tap
            setLeftSelected(!_isLeftSelected, animate = true)
        }

        // Expand touch area while preserving visual size
        post { expandTouchArea(24) } // 24px expansion; tweak if needed
    }

    fun setLeftSelected(left: Boolean, animate: Boolean = true) {
        if (_isLeftSelected == left) return
        _isLeftSelected = left
        updateVisualState(animate)
        listener?.invoke(_isLeftSelected)
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED)
        contentDescription = "Language ${if (_isLeftSelected) "English" else "Afrikaans"}"
    }

    private fun updateVisualState(animate: Boolean) {
        val target = if (_isLeftSelected) 0f else thumbTravelPx
        thumbText.text = if (_isLeftSelected) "English" else "Afrikaans"
        // note: in design the right label stays "Afrikaans" in place; we keep it unchanged
        if (animate) {
            thumb.animate()
                .translationX(target)
                .setInterpolator(interpolator)
                .setDuration(animDuration)
                .start()
        } else {
            thumb.translationX = target
        }
    }

    fun setOnSelectionChangedListener(l: (Boolean) -> Unit) {
        listener = l
    }

    private fun expandTouchArea(extraPx: Int) {
        val parentView = parent as? View ?: return
        val rect = Rect()
        getHitRect(rect)
        rect.left -= extraPx
        rect.top -= extraPx
        rect.right += extraPx
        rect.bottom += extraPx
        parentView.touchDelegate = TouchDelegate(rect, this)
    }

    // Save / restore checked state
    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.leftSelected = _isLeftSelected
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            _isLeftSelected = state.leftSelected
            updateVisualState(animate = false)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private class SavedState : BaseSavedState {
        var leftSelected: Boolean = true
        constructor(superState: Parcelable?) : super(superState)
        private constructor(parcel: Parcel) : super(parcel) {
            leftSelected = parcel.readInt() == 1
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeInt(if (leftSelected) 1 else 0)
        }

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(parcel: Parcel): SavedState = SavedState(parcel)
                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
            }
        }
    }
}


