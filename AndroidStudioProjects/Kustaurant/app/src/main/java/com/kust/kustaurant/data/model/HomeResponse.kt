package com.kust.kustaurant.data.model

data class HomeResponse(
    val topRestaurantsByRating: List<RestaurantResponse>,
    val restaurantsForMe: List<RestaurantResponse>,
    val photoUrls: List<String>
)