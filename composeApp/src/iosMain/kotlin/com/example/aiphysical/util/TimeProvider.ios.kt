package com.example.aiphysical.util

import platform.CoreFoundation.CFAbsoluteTimeGetCurrent

private const val APPLE_REFERENCE_TO_UNIX_SECONDS = 978_307_200.0

actual fun currentTimeMillis(): Long =
	((CFAbsoluteTimeGetCurrent() + APPLE_REFERENCE_TO_UNIX_SECONDS) * 1000.0).toLong()

