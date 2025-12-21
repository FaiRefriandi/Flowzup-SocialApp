package com.frzterr.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.frzterr.app.R

class ProfileTabFragment : androidx.fragment.app.Fragment() {

    enum class TabType {
        POSTS, REPOSTS
    }

    private var tabType: TabType = TabType.POSTS
    private var recyclerView: RecyclerView? = null
    private var emptyState: LinearLayout? = null
    private var scrollView: androidx.core.widget.NestedScrollView? = null
    private var shimmerViewContainer: com.facebook.shimmer.ShimmerFrameLayout? = null

    companion object {
        private const val ARG_TAB_TYPE = "tab_type"

        fun newInstance(tabType: TabType): ProfileTabFragment {
            return ProfileTabFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TAB_TYPE, tabType.ordinal)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tabType = TabType.values()[it.getInt(ARG_TAB_TYPE, 0)]
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.rvTabContent)
        emptyState = view.findViewById(R.id.emptyState)
        scrollView = view.findViewById(R.id.scrollView)
        shimmerViewContainer = view.findViewById(R.id.shimmerViewContainer)

        // Set empty message based on tab type
        view.findViewById<TextView>(R.id.tvEmptyMessage).text = when (tabType) {
            TabType.POSTS -> "Belum ada postingan"
            TabType.REPOSTS -> "Belum ada repost"
        }
    }

    fun setAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView?.adapter = adapter
        // Update scrollability after adapter is set and items are laid out
        recyclerView?.post { updateScrollability() }
    }

    fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            shimmerViewContainer?.visibility = View.VISIBLE
            shimmerViewContainer?.startShimmer()
            recyclerView?.visibility = View.GONE
            emptyState?.visibility = View.GONE
        } else {
            shimmerViewContainer?.stopShimmer()
            shimmerViewContainer?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
            
            // Re-check empty state if needed, can be handled by caller calling updateEmptyState
        }
    }

    fun updateEmptyState(isEmpty: Boolean) {
        // Only show empty state if NOT loading
        if (shimmerViewContainer?.visibility != View.VISIBLE) {
            emptyState?.visibility = if (isEmpty) View.VISIBLE else View.GONE
            // Update scroll after visibility change
            scrollView?.post { updateScrollability() }
        }
    }
    
    /**
     * Dynamically enable/disable scroll based on actual content height
     * Only scrollable if content exceeds viewport height
     */
    private fun updateScrollability() {
        scrollView?.post {
            // Get the actual content height (RecyclerView or EmptyState)
            val contentHeight = if (emptyState?.visibility == View.VISIBLE) {
                // Empty state height is minimal, no scroll needed
                emptyState?.height ?: 0
            } else {
                // Get RecyclerView's total content height
                recyclerView?.height ?: 0
            }
            
            val scrollViewHeight = scrollView?.height ?: 0
            
            // Enable scroll only if content is larger than viewport
            val shouldScroll = contentHeight > scrollViewHeight
            
            // Block touch when scroll is not needed
            scrollView?.setOnTouchListener { _, _ -> !shouldScroll }
            scrollView?.isNestedScrollingEnabled = shouldScroll
        }
    }

    fun getTabType(): TabType = tabType
}
