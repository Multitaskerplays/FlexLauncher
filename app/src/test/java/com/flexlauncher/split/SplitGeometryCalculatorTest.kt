package com.flexlauncher.split

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The car case is a fixed geometry, so assert the numbers instead of eyeballing them:
 * Carlinkit Ultra AI Box, 2270x1016 app area behind an XPeng G6's CarPlay card.
 */
class SplitGeometryCalculatorTest {

    /** 63% of the 2374 px panel is the card edge, expressed in the 2270 px app area. */
    private fun carDividerPx(): Int = 2374 * SplitGeometryCalculator.DEFAULT_RATIO_PCT / 100

    @Test
    fun `car defaults put the divider on the card edge`() {
        val geometry = SplitGeometryCalculator.bounds(
            width = 2270,
            height = 1016,
            dividerPx = carDividerPx(),
            bottomInsetPx = SplitGeometryCalculator.DEFAULT_BOTTOM_INSET_PX,
            overscanPx = SplitGeometryCalculator.DEFAULT_OVERSCAN_PX
        )

        assertEquals(1495, geometry.dividerPx)
        assertEquals(-10, geometry.left.left)
        assertEquals(1495, geometry.left.right)
        assertEquals(0, geometry.left.top)
        assertEquals(971, geometry.left.bottom)

        assertEquals(1495, geometry.right.left)
        assertEquals(2280, geometry.right.right)
        assertEquals(971, geometry.right.bottom)
    }

    @Test
    fun `panes meet exactly at the divider and never overlap`() {
        for (pct in 20..90 step 5) {
            val divider = (2374 * pct / 100).coerceIn(1, 2269)
            val geometry = SplitGeometryCalculator.bounds(2270, 1016, divider, 45, 10)
            assertEquals(geometry.dividerPx, geometry.left.right)
            assertEquals(geometry.dividerPx, geometry.right.left)
        }
    }

    @Test
    fun `ratio is clamped to something usable`() {
        // a divider outside the app area is clamped to it
        assertEquals(1, SplitGeometryCalculator.bounds(2270, 1016, 0, 0, 0).dividerPx)
        assertEquals(2269, SplitGeometryCalculator.bounds(2270, 1016, 99999, 0, 0).dividerPx)
    }
}
