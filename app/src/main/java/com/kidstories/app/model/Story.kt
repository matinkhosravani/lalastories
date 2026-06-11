package com.kidstories.app.model

data class Page(
    val pageNumber: Int,
    val text: String,
    val image: String? = null,
    val imagePath: String? = null
)

data class Story(
    val id: String,
    val title: String,
    val description: String,
    val ageMin: Int,
    val ageMax: Int,
    val pages: List<Page>,
    val coverPath: String? = null,
    val audioPath: String? = null
)
