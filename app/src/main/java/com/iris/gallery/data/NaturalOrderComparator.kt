package com.iris.gallery.data

/**
 * NaturalOrderComparator performs human-natural alphanumeric string comparison.
 * Strings with numbers are sorted by their numerical magnitude (e.g. "IMG_1", "IMG_2", "IMG_10"
 * rather than ASCII order "IMG_1", "IMG_10", "IMG_2").
 */
object NaturalOrderComparator : Comparator<String> {
    override fun compare(s1: String, s2: String): Int {
        var i1 = 0
        var i2 = 0
        val len1 = s1.length
        val len2 = s2.length

        while (i1 < len1 && i2 < len2) {
            val c1 = s1[i1]
            val c2 = s2[i2]

            if (c1.isDigit() && c2.isDigit()) {
                val start1 = i1
                while (i1 < len1 && s1[i1].isDigit()) i1++
                val start2 = i2
                while (i2 < len2 && s2[i2].isDigit()) i2++

                // Skip leading zeros
                var nz1 = start1
                while (nz1 < i1 - 1 && s1[nz1] == '0') nz1++
                var nz2 = start2
                while (nz2 < i2 - 1 && s2[nz2] == '0') nz2++

                val numLen1 = i1 - nz1
                val numLen2 = i2 - nz2

                if (numLen1 != numLen2) {
                    return numLen1 - numLen2
                }

                for (k in 0 until numLen1) {
                    val d1 = s1[nz1 + k]
                    val d2 = s2[nz2 + k]
                    if (d1 != d2) return d1 - d2
                }

                // If numerical value is identical, compare leading zeroes count
                val leadZero1 = nz1 - start1
                val leadZero2 = nz2 - start2
                if (leadZero1 != leadZero2) return leadZero1 - leadZero2
            } else {
                val comp = c1.lowercaseChar().compareTo(c2.lowercaseChar())
                if (comp != 0) return comp
                i1++
                i2++
            }
        }
        return len1 - len2
    }
}
