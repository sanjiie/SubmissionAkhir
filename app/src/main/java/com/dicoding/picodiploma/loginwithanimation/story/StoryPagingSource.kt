package com.dicoding.picodiploma.loginwithanimation.story

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.dicoding.picodiploma.loginwithanimation.api.ApiService

class StoryPagingSource(
    private val apiService: ApiService,
    private val token: String
) : PagingSource<Int, ListStoryItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ListStoryItem> {
        return try {
            val position = params.key ?: 1
            Log.d("StoryPagingSource", "Loading page $position with size ${params.loadSize}")
            val response = apiService.getStories(page = position, size = params.loadSize)
            val stories = response.listStory?.filterNotNull() ?: emptyList()
            Log.d("StoryPagingSource", "Loaded ${stories.size} stories")
            LoadResult.Page(
                data = stories,
                prevKey = if (position == 1) null else position - 1,
                nextKey = if (stories.isEmpty()) null else position + 1
            )
        } catch (e: Exception) {
            Log.e("StoryPagingSource", "Error loading data: ${e.message}")
            LoadResult.Error(e)
        }
    }


    override fun getRefreshKey(state: PagingState<Int, ListStoryItem>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }
}

