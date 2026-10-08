package com.gabs.cubo3x3.data.custom

import com.gabs.cubo3x3.cube.CubeColorScheme
import com.gabs.cubo3x3.cube.CubeViewpoint

data class CustomAlgorithm(
    val id: Long,
    val name: String,
    val notation: String,
    val tags: List<String>,
    val colorScheme: CubeColorScheme,
    val viewpoint: CubeViewpoint,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
