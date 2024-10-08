package com.kust.kustaurant.presentation.ui.tier

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kust.kustaurant.R
import com.kust.kustaurant.databinding.FragmentTierListBinding
import com.kust.kustaurant.domain.model.TierRestaurant
import com.kust.kustaurant.presentation.ui.detail.DetailActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TierListFragment : Fragment() {
    private lateinit var binding: FragmentTierListBinding
    private val viewModel: TierViewModel by activityViewModels()
    private lateinit var tierAdapter: TierListAdapter
    private val allTierData = mutableListOf<TierRestaurant>()
    private var recyclerViewState: Parcelable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTierListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        if ((viewModel.selectedMenus.value ?: emptySet()) == setOf("") &&
            (viewModel.selectedSituations.value ?: emptySet()) == setOf("") &&
            (viewModel.selectedLocations.value ?: emptySet()) == setOf("")
        ) {
            viewModel.applyFilters(setOf("ALL"), setOf("ALL"), setOf("ALL"), 0)
        }

        setupRecyclerView()

        binding.tierSrl.setOnRefreshListener {
            refreshData()
        }

        binding.tierRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // 스크롤이 끝에 도달했는지 확인
                if (!recyclerView.canScrollVertically(1)) {
                    viewModel.checkAndLoadBackendListData(TierViewModel.Companion.RestaurantState.NEXT_PAGE_LIST_DATA)
                }
            }
        })
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState != null) {
            recyclerViewState = savedInstanceState.getParcelable("recyclerViewState")
        }

        observeViewModel()
    }

    private fun setupRecyclerView() {
        tierAdapter = TierListAdapter(requireContext())
        binding.tierRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tierAdapter
        }

        tierAdapter.setOnItemClickListener(object : TierListAdapter.OnItemClickListener {
            override fun onItemClicked(data: TierRestaurant) {
                val intent = Intent(requireActivity(), DetailActivity::class.java)
                val options = ActivityOptions.makeCustomAnimation(requireContext(), R.anim.slide_in_right, R.anim.stay_in_place)
                intent.putExtra("restaurantId", data.restaurantId)
                startActivity(intent, options.toBundle())
            }
        })
    }

    private fun observeViewModel() {
        viewModel.allTierRestaurantList.observe(viewLifecycleOwner) { tierList ->
            if (viewModel.isSelectedCategoriesChanged.value == true) {
                allTierData.clear()
            }

            Log.e("TierFragment", viewModel.isSelectedCategoriesChanged.toString())

            allTierData.addAll(tierList)
            tierAdapter.submitList(allTierData.toList())
        }

        viewModel.isExpanded.observe(viewLifecycleOwner) { isExpanded ->
            tierAdapter.setExpanded(isExpanded)
            val text = if (isExpanded) getString(R.string.small_btn_info) else getString(R.string.wide_btn_info)
            val content = SpannableString(text)
            content.setSpan(UnderlineSpan(), 0, text.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            binding.tierTvToggleExtendReduce.text = content
        }
    }

    private fun refreshData() {
        allTierData.clear()
        viewModel.checkAndLoadBackendListData(TierViewModel.Companion.RestaurantState.RELOAD_RESTAURANT_LIST_DATA)
        binding.tierRecyclerView.scrollToPosition(0)
        binding.tierSrl.isRefreshing = false
    }

    override fun onResume() {
        super.onResume()

        if (recyclerViewState != null) {
            binding.tierRecyclerView.layoutManager?.onRestoreInstanceState(recyclerViewState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(
            "recyclerViewState",
            binding.tierRecyclerView.layoutManager?.onSaveInstanceState()
        )
    }
}