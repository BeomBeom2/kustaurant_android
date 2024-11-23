package com.kust.kustaurant.presentation.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CommunityPostIntent(
    val postId: Int,
    val postTitle: String,
    val postCategory: String,
    val postBody: String
) : Parcelable
