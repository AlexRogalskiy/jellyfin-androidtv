package org.jellyfin.androidtv.data.itemtypes

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.ImageOptions
import org.jellyfin.apiclient.model.entities.ImageType
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import timber.log.Timber

class ImageCollection(original: BaseItemDto) {
	val primary = original.imageTags[ImageType.Primary]?.let { Image(original.id, ImageType.Primary, it) }
	val logo = original.imageTags[ImageType.Logo]?.let { Image(original.id, ImageType.Logo, it) }
	val backdrops = original.backdropImageTags.map { Image(original.id, ImageType.Backdrop, it) }.toList()
	val parentPrimary = original.parentPrimaryImageItemId?.let { Image(original.parentId, ImageType.Primary, it) }
	val parentBackdrops = original.parentBackdropImageTags?.let { it.map { Image(original.parentBackdropItemId, ImageType.Backdrop, it) }.toList() }

	class Image(
			private val itemId: String,
			private val type: ImageType,
			private val tag: String?,
			private val index: Int? = null
	) : KoinComponent {
		val url: String by lazy {
			get<ApiClient>().GetImageUrl(itemId, ImageOptions().also {
				it.imageType = type
				it.tag = tag
				it.imageIndex = index
				it.enableImageEnhancers = false
			})
		}

		suspend fun getBitmap(context: Context) = withContext(Dispatchers.IO) {
			Timber.i("getBitmap() URL: $url")
			Glide.with(context)
					.asBitmap()
					.load(url)
					.submit()
					.get()
		}

		fun load(context: Context, success: (bitmap: Bitmap) -> Unit) {
			Timber.i("load() URL: $url")
			GlobalScope.launch(Dispatchers.IO) {
				val bitmap = Glide.with(context)
						.asBitmap()
						.load(url)
						.submit()
						.get()
				withContext(Dispatchers.Main) { success(bitmap) }
			}
		}
	}
}
