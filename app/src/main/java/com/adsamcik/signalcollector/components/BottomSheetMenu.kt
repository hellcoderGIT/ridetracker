package com.adsamcik.signalcollector.components

import android.support.annotation.ColorInt
import android.support.annotation.StringRes
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import com.adsamcik.signalcollector.R
import com.adsamcik.signalcollector.extensions.dpAsPx
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Implementation of https://material.io/guidelines/components/bottom-sheets.html
 *
 * It is basically a menu that is in the bottom and can be pulled up.
 * Menu does not allow removal of items.
 */
class BottomSheetMenu(root: CoordinatorLayout) {
    private val menuRoot: LinearLayout
    private val menuItems: ArrayList<Button>
    private val bottomSheetBehavior: BottomSheetBehavior<*>

    @ColorInt
    private val textColor: Int

    init {
        val context = root.context

        LayoutInflater.from(context).inflate(R.layout.bottom_sheet_menu, root)
        menuRoot = root.getChildAt(root.childCount - 1) as LinearLayout
        bottomSheetBehavior = BottomSheetBehavior.from(menuRoot)
        bottomSheetBehavior.peekHeight = 54.dpAsPx
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        menuItems = ArrayList()
        val typedValue = TypedValue()
        //inverse text color returned weird values
        context.theme.resolveAttribute(R.attr.titleColor, typedValue, true)
        textColor = typedValue.data
    }

    /**
     * Add item to the menu
     *
     * @param title String resource of the title
     * @param onClickListener On click listener for the item
     */
    fun addItem(@StringRes title: Int, onClickListener: View.OnClickListener) {
        val context = menuRoot.context
        val button = Button(context)
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.selectableItemBackground, typedValue, true)
        button.setBackgroundResource(typedValue.resourceId)
        button.setOnClickListener(onClickListener)
        button.setText(title)
        button.setTextColor(textColor)
        menuItems.add(button)

        menuRoot.addView(button)
    }

    /**
     * Special behavior to help user understand what this menu is about. It shows the whole menu for a moment before collapsing
     */
    fun showHide(delayInMS: Int) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        launch {
            delay(delayInMS.toLong(), TimeUnit.MILLISECONDS)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }
}
