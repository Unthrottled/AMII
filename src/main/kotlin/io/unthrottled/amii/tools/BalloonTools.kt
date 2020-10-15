package io.unthrottled.amii.tools

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFrame
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import java.awt.Point

object BalloonTools {
  private const val NOTIFICATION_Y_OFFSET = 20
  fun fetchBalloonParameters(project: Project): Pair<IdeFrame, RelativePoint> {
    val ideFrame = WindowManager.getInstance().getIdeFrame(project)
      ?: WindowManager.getInstance().allProjectFrames.first()
    val frameBounds = ideFrame.component.bounds
    val notificationPosition = RelativePoint(
      ideFrame.component,
      Point(frameBounds.x + frameBounds.width, NOTIFICATION_Y_OFFSET)
    )
    return Pair(ideFrame, notificationPosition)
  }
}
