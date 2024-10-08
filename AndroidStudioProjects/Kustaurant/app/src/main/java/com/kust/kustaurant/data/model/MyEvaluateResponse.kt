package com.kust.kustaurant.data.model

data class MyEvaluateResponse (
    val restaurantName: String,
    val restaurantId : Int,
    val restaurantImgURL: String,
    val cuisine: String,
    val evaluationScore: Double,
    val restaurantComment : String,
    val evaluationItemScores : ArrayList<String>?
)