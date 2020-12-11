package io.unthrottled.amii.assets

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.unthrottled.amii.assets.AssetCategory.VISUALS
import io.unthrottled.amii.tools.Logging
import io.unthrottled.amii.tools.logger
import io.unthrottled.amii.tools.runSafelyWithResult
import io.unthrottled.amii.tools.toOptional
import java.net.URI
import java.util.Optional

object VisualAssetManager :
  RemoteAssetManager<VisualMemeAssetDefinition, VisualMemeAsset>(
    VISUALS
  ),
  Logging {

  override fun convertToAsset(
    asset: VisualMemeAssetDefinition,
    assetUrl: URI
  ): VisualMemeAsset =
    asset.toAsset(assetUrl)

  override fun convertToDefinitions(defJson: String): Optional<List<VisualMemeAssetDefinition>> =
    runSafelyWithResult({
      Gson().fromJson<List<VisualMemeAssetDefinition>>(
        defJson,
        object : TypeToken<List<VisualMemeAssetDefinition>>() {}.type
      ).toOptional()
    }) {
      logger().warn("Unable to read Visual Assets for reasons $defJson", it)
      Optional.empty()
    }
}
