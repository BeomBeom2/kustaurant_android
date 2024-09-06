package com.kust.kustaurant.presentation.ui.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kust.kustaurant.R
import com.kust.kustaurant.data.getAccessToken
import com.kust.kustaurant.databinding.FragmentDetailReviewBinding
import com.kust.kustaurant.presentation.ui.splash.StartActivity

class DetailReviewFragment : Fragment() {
    lateinit var binding : FragmentDetailReviewBinding
    lateinit var reviewAdapter: DetailReviewAdapter
    private val viewModel: DetailViewModel by activityViewModels()
    private var restaurantId = 0
    private val popularity = "popularity"
    private val latest = "latest"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDetailReviewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        initRecyclerView()
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments != null) {
            restaurantId = requireArguments().getInt("restaurantId", 0)
            Log.d("restaurantId", restaurantId.toString())
        }

        updateButton(binding.detailBtnPopular)

        binding.detailBtnRecent.setOnClickListener {
            updateButton(it)
            viewModel.loadCommentData(restaurantId, latest)
        }

        binding.detailBtnPopular.setOnClickListener {
            updateButton(it)
            viewModel.loadCommentData(restaurantId, popularity)
        }

        observeViewModel()
        viewModel.loadCommentData(restaurantId, popularity)
    }


    fun checkToken(action: () -> Unit) {
        val accessToken = getAccessToken(requireContext())
        if (accessToken == null) {
            val intent = Intent(context, StartActivity::class.java)
            startActivity(intent)
        } else {
            action()
        }
    }

    private fun updateButton(selectedView: View?) {
        val views = listOf(binding.detailBtnRecent, binding.detailBtnPopular)
        val textViews = listOf(binding.detailTvRecent, binding.detailTvPopular)

        for (index in views.indices) {
            val view = views[index]
            val textView = textViews[index]
            val isSelected = view == selectedView

            view.isSelected = isSelected
            textView.isSelected = isSelected
        }
    }

    private fun observeViewModel() {
        viewModel.reviewData.observe(viewLifecycleOwner) { commentData ->
            if (commentData.isEmpty()) {
                binding.detailBtnRecent.visibility = View.GONE
                binding.detailBtnPopular.visibility = View.GONE
                binding.detailClReviewNone.visibility = View.VISIBLE
                setNoneHeight()
            } else {
                reviewAdapter.submitList(commentData) {
                    binding.detailRvReview.post {
                        setRecyclerViewHeight()
                    }
                }
            }
        }
    }


    override fun onStart() {
        super.onStart()
        Log.d("life","review start")
    }


    override fun onResume() {
        super.onResume()
        Log.d("life","review resume")
        viewModel.loadCommentData(restaurantId, popularity)
        Log.d("reviewData", viewModel.loadCommentData(restaurantId, popularity).toString())

    }

    private fun initRecyclerView() {
        reviewAdapter = DetailReviewAdapter(requireContext())

        binding.detailRvReview.adapter = reviewAdapter
        binding.detailRvReview.layoutManager = LinearLayoutManager(requireContext())
        binding.detailRvReview.itemAnimator = null

        reviewAdapter.setOnItemClickListener(object : DetailReviewAdapter.OnItemClickListener {
            override fun onReportClicked(commentId: Int) {
                checkToken{
                    viewModel.postCommentReport(restaurantId, commentId)
                    Toast.makeText(requireContext(), "신고가 접수되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onDeleteClicked(commentId: Int) {
                checkToken{
                    viewModel.deleteCommentData(restaurantId, commentId)
                    Toast.makeText(requireContext(), "댓글 삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                    setRecyclerViewHeight()
                }
            }

            override fun onCommentClicked(commentId: Int) {
                checkToken{
                    showBottomSheetInput(commentId)
                }
            }

            override fun onLikeClicked(commentId: Int, position: Int) {
                checkToken{
                    viewModel.postCommentLike(restaurantId, commentId, position)
                }
            }

            override fun onDisLikeClicked(commentId: Int, position: Int) {
                checkToken{
                    viewModel.postCommentDisLike(restaurantId, commentId, position)
                }
            }
        })

        reviewAdapter.interactionListener = object : DetailRelyAdapter.OnItemClickListener{
            override fun onReportClicked(commentId: Int) {
                checkToken{
                    viewModel.postCommentReport(restaurantId, commentId)
                    Toast.makeText(requireContext(), "신고가 접수되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onDeleteClicked(commentId: Int) {
                checkToken{
                    viewModel.deleteCommentData(restaurantId, commentId)
                    Toast.makeText(requireContext(), "댓글 삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                    setRecyclerViewHeight()
                }
            }

            override fun onLikeClicked(commentId: Int, position: Int) {
                checkToken{
                    viewModel.postCommentLike(restaurantId, commentId, position)
                }
            }

            override fun onDisLikeClicked(commentId: Int, position: Int) {
                checkToken{
                    viewModel.postCommentDisLike(restaurantId, commentId, position)
                }
            }
        }
    }

    private fun showBottomSheetInput(commentId: Int) {
        val bottomSheetDialog = BottomSheetDialog(requireContext()).apply {
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_comment, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        val etInput = bottomSheetView.findViewById<EditText>(R.id.detail_et_input)
        val btnSubmit = bottomSheetView.findViewById<ConstraintLayout>(R.id.detail_cl_comment_confirm)

        bottomSheetDialog.setOnShowListener {
            etInput.requestFocus()
            // 바텀 sheet 생성하는데 시간 지연
            etInput.postDelayed({
                val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(etInput, InputMethodManager.SHOW_IMPLICIT)
            }, 100)
        }

        btnSubmit.setOnClickListener {
            val inputText = etInput.text.toString()
            if (inputText.isNotBlank()) {
                viewModel.postCommentData(restaurantId, commentId, inputText)
                bottomSheetDialog.dismiss()
                Toast.makeText(requireContext(), "대댓글이 등록되었습니다.", Toast.LENGTH_SHORT).show()
                setRecyclerViewHeight()
            } else {
                etInput.error = "텍스트를 입력해주세요"
                Toast.makeText(requireContext(), "텍스트를 입력해주세요", Toast.LENGTH_SHORT).show()
            }
        }

        bottomSheetDialog.setOnDismissListener {
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etInput.windowToken, 0)
        }

        bottomSheetDialog.show()
    }

    // 각 tab마다 height 재구성
    private fun setRecyclerViewHeight() {
        binding.detailRvReview.post {
            // RecyclerView의 전체 내용 높이 계산
            val recyclerHeight = binding.detailRvReview.computeVerticalScrollRange()

            // 버튼들의 높이 계산 (예시에서는 버튼 2개의 높이를 합산)
            val buttonsHeight = binding.detailBtnPopular.height + binding.detailBtnRecent.height

            // 전체 뷰의 높이는 RecyclerView 높이 + 버튼 높이
            val totalHeight = recyclerHeight + buttonsHeight

            // RecyclerView 레이아웃 파라미터 업데이트
            val params = binding.detailRvReview.layoutParams
            params.height = totalHeight
            binding.detailRvReview.layoutParams = params

            // 부모 뷰 또는 연관된 뷰의 높이를 조절해야 할 경우
            (activity as? DetailActivity)?.setViewPagerHeight(totalHeight)
        }
    }

    private fun setNoneHeight() {
        val params = binding.detailClReviewNone.layoutParams
        params.height = 700
        binding.detailClReviewNone.layoutParams = params

        (activity as? DetailActivity)?.setViewPagerHeight(700)
    }
}