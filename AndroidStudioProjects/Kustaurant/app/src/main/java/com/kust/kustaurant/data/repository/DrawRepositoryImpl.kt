package com.kust.kustaurant.data.repository

import com.kust.kustaurant.data.model.DrawRestaurantData
import com.kust.kustaurant.data.remote.TierApi
import com.kust.kustaurant.domain.repository.DrawRepository
import javax.inject.Inject

class DrawRepositoryImpl @Inject constructor(
    private val tierApi: TierApi
) : DrawRepository {
    override suspend fun getDrawRestaurantData(cuisines: String, locations: String): List<DrawRestaurantData> {
        return tierApi.getDrawRestaurantsData(cuisines, locations)
    }
}
