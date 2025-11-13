package com.example.deflate

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.TextView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

class NotificationToggleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val thumb: View
    private val thumbText: TextView
    private val track: View

    // dimensions in pixels (computed from dp)
    private val trackWidthPx: Float
    private val thumbWidthPx: Float
    private val thumbTravelPx: Float

    private val animDuration = 200L
    private val interpolator = FastOutSlowInInterpolator()

    private var _isChecked = true
    var isChecked: Boolean
        get() = _isChecked
        set(value) {
            setChecked(value, animate = false)
        }

    private var listener: ((Boolean) -> Unit)? = null

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.view_notification_toggle, this, true)

        track = findViewById(R.id.notification_track)
        thumb = findViewById(R.id.notification_thumb)
        thumbText = findViewById(R.id.notification_thumb_text)

        val density = resources.displayMetrics.density
        trackWidthPx = 140f * density
        thumbWidthPx = 76f * density
        thumbTravelPx = (trackWidthPx - thumbWidthPx)

        setChecked(_isChecked, animate = false)

        isClickable = true
        isFocusable = true
        contentDescription = "Notifications toggle"

        setOnClickListener {
            setChecked(!_isChecked, animate = true)
        }
    }

    fun setChecked(checked: Boolean, animate: Boolean = true) {
        if (_isChecked == checked) return
        _isChecked = checked
        updateVisualState(animate)
        listener?.invoke(_isChecked)

        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED)
        contentDescription = "Notifications ${if (_isChecked) "On" else "Off"}"
    }

    private fun updateVisualState(animate: Boolean) {
        val target = if (_isChecked) 0f else thumbTravelPx

        thumbText.text = if (_isChecked) "On" else "Off"
        
        // position & animate
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

    fun setOnCheckedChangeListener(l: (Boolean) -> Unit) {
        listener = l
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.checked = _isChecked
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            _isChecked = state.checked
            updateVisualState(animate = false)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private class SavedState : BaseSavedState {
        var checked: Boolean = true
        constructor(superState: Parcelable?) : super(superState)
        private constructor(parcel: Parcel) : super(parcel) {
            checked = parcel.readInt() == 1
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeInt(if (checked) 1 else 0)
        }

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(parcel: Parcel): SavedState {
                    return SavedState(parcel)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}

