package com.adamfoerster.syncthing.core.presentation

import java.text.DecimalFormatSymbols

actual fun systemDecimalSeparator(): Char = DecimalFormatSymbols.getInstance().decimalSeparator
