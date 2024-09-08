package com.kust.kustaurant.presentation.ui.detail

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kust.kustaurant.data.model.CommentDataResponse
import com.kust.kustaurant.data.model.CommentLikeResponse
import com.kust.kustaurant.data.model.DetailDataResponse
import com.kust.kustaurant.data.model.EvaluationDataRequest
import com.kust.kustaurant.data.model.EvaluationDataResponse
import com.kust.kustaurant.domain.usecase.detail.DeleteCommentDataUseCase
import com.kust.kustaurant.domain.usecase.detail.GetCommentDataUseCase
import com.kust.kustaurant.domain.usecase.detail.GetDetailDataUseCase
import com.kust.kustaurant.domain.usecase.detail.GetEvaluationDataUseCase
import com.kust.kustaurant.domain.usecase.detail.PostCommentDataUseCase
import com.kust.kustaurant.domain.usecase.detail.PostCommentDisLikeUseCase
import com.kust.kustaurant.domain.usecase.detail.PostCommentLikeUseCase
import com.kust.kustaurant.domain.usecase.detail.PostCommentReportUseCase
import com.kust.kustaurant.domain.usecase.detail.PostEvaluationDataUseCase
import com.kust.kustaurant.domain.usecase.detail.PostFavoriteToggleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.w3c.dom.Text
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getDetailDataUseCase: GetDetailDataUseCase,
    private val getCommentDataUseCase: GetCommentDataUseCase,
    private val postCommentDataUseCase: PostCommentDataUseCase,
    private val postFavoriteToggleUseCase: PostFavoriteToggleUseCase,
    private val getEvaluationDataUseCase: GetEvaluationDataUseCase,
    private val postEvaluationDataUseCase: PostEvaluationDataUseCase,
    private val deleteCommentDataUseCase: DeleteCommentDataUseCase,
    private val postCommentLikeUseCase: PostCommentLikeUseCase,
    private val postCommentDisLikeUseCase: PostCommentDisLikeUseCase,
    private val postCommentReportUseCase : PostCommentReportUseCase
): ViewModel() {
    val tabList = MutableLiveData(listOf("메뉴", "리뷰"))

    private val _detailData = MutableLiveData<DetailDataResponse>()
    val detailData: LiveData<DetailDataResponse> = _detailData

    private val _menuData = MutableLiveData<List<MenuData>>()
    val menuData: LiveData<List<MenuData>> = _menuData

    private val _tierData = MutableLiveData<TierInfoData>()
    val tierData: LiveData<TierInfoData> = _tierData

    private var _reviewData = MutableLiveData<List<CommentDataResponse>>()
    val reviewData: LiveData<List<CommentDataResponse>> = _reviewData

    private val _favoriteData = MutableLiveData<Boolean>()
    val favoriteData: LiveData<Boolean> = _favoriteData

    private val _evaluationData = MutableLiveData<EvaluationDataResponse>()
    val evaluationData: LiveData<EvaluationDataResponse> = _evaluationData

    private val _pEvaluationData = MutableLiveData<DetailDataResponse>()
    val pEvaluationData: LiveData<DetailDataResponse> = _pEvaluationData

    private val _itemUpdateIndex = MutableLiveData<Int>()
    val itemUpdateIndex: LiveData<Int> = _itemUpdateIndex

    val evaluationComplete = MutableLiveData<Boolean>()
    private var currentSort = "popularity"

    fun loadDetailData(restaurantId : Int) {
        viewModelScope.launch {
            try {
                val getDetailData = getDetailDataUseCase(restaurantId)
                _detailData.value = getDetailData
                _menuData.value = getDetailData.restaurantMenuList.map {
                    MenuData(it.menuId, it.menuName, it.menuPrice, it.naverType, it.menuImgUrl)
                }
                _tierData.value = TierInfoData(
                    getDetailData.restaurantCuisineImgUrl, getDetailData.restaurantCuisine,
                    getDetailData.mainTier, getDetailData.situationList
                )
            } catch (e : Exception) {
                Log.e("DetailViewModel", "Failed to load detail data", e)
            }
        }
    }

    fun loadCommentData(restaurantId: Int, sort: String){
        currentSort = sort
        viewModelScope.launch {
            try{
                val getCommentData = getCommentDataUseCase(restaurantId, sort)
                _reviewData.postValue(getCommentData.toList())
            } catch (e: Exception){
                Log.e("CommentLoad", "Failed to load comment", e)
            }
        }
    }

    fun loadEvaluateData(restaurantId: Int){
        viewModelScope.launch {
            try {
                val getDetailData = getDetailDataUseCase(restaurantId)
                _detailData.value = getDetailData
            } catch (e: Exception){
                Log.e("DetailViewModel", "Failed to load evaluation data", e)
            }
        }
    }

    fun postCommentData(restaurantId: Int, commentId: Int, inputText: String) {
        viewModelScope.launch {
            try {
                postCommentDataUseCase(restaurantId, commentId, inputText)
                loadCommentData(restaurantId, currentSort)
            } catch (e: Exception) {
                Log.e("CommentPost", "Failed to post comment", e)
            }
        }
    }

    fun postFavoriteToggle(restaurantId: Int) {
        viewModelScope.launch {
            try {
                val newFavoriteState = postFavoriteToggleUseCase(restaurantId)

                _detailData.value?.let { currentDetailData ->
                    val updatedDetailData = currentDetailData.copy(
                        isFavorite = newFavoriteState,
                        favoriteCount = calculateFavoriteCount(currentDetailData, newFavoriteState)
                    )
                    _detailData.postValue(updatedDetailData)
                }
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Failed to toggle favorite", e)
            }
        }
    }

    private fun calculateFavoriteCount(currentDetailData: DetailDataResponse, newFavoriteState: Boolean): Int {
        return if (newFavoriteState) {
            currentDetailData.favoriteCount + 1
        } else {
            currentDetailData.favoriteCount - 1
        }
    }


    fun loadMyEvaluationData(restaurantId: Int){
        viewModelScope.launch {
            try {
                val evaluationData = getEvaluationDataUseCase(restaurantId)
                _evaluationData.postValue(evaluationData)
            } catch (e : Exception){
                Log.e("DetailViewModel", "Failed to MyloadEvaluation", e)
            }
        }
    }

    fun postEvaluationData(context: Context, restaurantId: Int, rating: Double, comment: String, keywords: List<Int>, imageUri: Uri?) {
        viewModelScope.launch {
            try {
                val ratingPart = rating.toString().toRequestBody(MultipartBody.FORM)
                val commentPart = comment.toRequestBody(MultipartBody.FORM)
                val keywordParts = keywords.map { keyword ->
                    MultipartBody.Part.createFormData("evaluationSituations", keyword.toString())
                }
                Log.d("taejung", keywords.toString())
                val imagePart: MultipartBody.Part? = imageUri?.let { uri ->
                    val file = getFileFromUri(context, uri)
                    val requestFile = file?.asRequestBody("image/jpg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("newImage", file?.name, requestFile!!)

                }
                postEvaluationDataUseCase(restaurantId, ratingPart, keywordParts, commentPart, imagePart)
                evaluationComplete.value = true
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Failed to post evaluation", e)
            }
        }
    }


    private fun getFileFromUri(context: Context, uri: Uri): File? {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("temp_image", ".jpg", context.cacheDir)
        tempFile.outputStream().use { outputStream ->
            inputStream?.copyTo(outputStream)
        }
        return tempFile
    }

    fun deleteCommentData(restaurantId: Int, commentId: Int) {
        viewModelScope.launch {
            try {
                deleteCommentDataUseCase(restaurantId, commentId)
                loadCommentData(restaurantId, currentSort)
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Failed to delete comment", e)
            }
        }
    }


    fun postCommentReport(restaurantId: Int, commentId: Int){
        try {
            viewModelScope.launch {
                postCommentReportUseCase(restaurantId, commentId)
            }
        } catch (e: Exception){
            Log.d("DetailViewModel", "post report", e)
        }
    }

    fun postCommentLike(restaurantId: Int, commentId: Int) {
        viewModelScope.launch {
            try {
                val response = postCommentLikeUseCase(restaurantId, commentId)
                updateCommentData(response, commentId)
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Failed to post comment like", e)
            }
        }
    }

    fun postCommentDisLike(restaurantId: Int, commentId: Int) {
        viewModelScope.launch {
            try {
                val response = postCommentDisLikeUseCase(restaurantId, commentId)
                updateCommentData(response, commentId)
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Failed to post comment dislike", e)
            }
        }
    }

    private fun updateCommentData(response: CommentLikeResponse, commentId: Int) {
        _reviewData.value?.let { currentReviews ->
            // 기존 reviewData를 MutableList로 나열
            val updatedReviews = currentReviews.map { review ->

                // commentId가 일치하면 review 데이터를 업데이트
                val updatedComment = if (review.commentId == commentId) {
                    review.copy(
                        commentLikeStatus = response.commentLikeStatus,
                        commentLikeCount = response.commentLikeCount,
                        commentDislikeCount = response.commentDislikeCount
                    )
                } else {
                    // commentId가 일치하지 않으면 대댓글을 업데이트
                    val updatedReplies = review.commentReplies?.map { reply ->

                        if (reply.commentId == commentId) {
                            reply.copy(
                                commentLikeStatus = response.commentLikeStatus,
                                commentLikeCount = response.commentLikeCount,
                                commentDislikeCount = response.commentDislikeCount
                            )
                        } else {
                            // 대댓글 까지 일치하지 않으면 그대로 반환
                            reply
                        }
                    }
                    // 업데이트한 reply Data 를 reviewData로 복사
                    review.copy(commentReplies = updatedReplies)
                }

                // 업데이트된 댓글, 대댓글 반환
                updatedComment
            }
            // 변경된 reviewData를 LiveData에 업데이트
            _reviewData.postValue(updatedReviews)
        }
    }

}