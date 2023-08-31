/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 hsz Jakub Chrzanowski <jakub@hsz.mobi>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
@file:Suppress("unused", "SpellCheckingInspection")

package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import javax.swing.Icon

open class CaosNotificationsBase(
    groupName: String
) {
    private val notificationGroup: NotificationGroup by lazy {
        NotificationGroupManager
            .getInstance()
            .getNotificationGroup(groupName)
    }

    /**
     * Shows {@link Notification} in ".ignore plugin" group.
     *
     * @param project   current project
     * @param title     notification title
     * @param content   notification text
     * @param type      notification type
     */
    fun show(
        project: Project, title: String, content: String,
        type: NotificationType,
    ) {
        show(project, title, content, notificationGroup, type, null)
    }

    /**
     * Shows {@link Notification} in ".ignore plugin" group.
     *
     * @param project   current project
     * @param title     notification title
     * @param content   notification text
     */
    fun showWarning(project: Project, title: String, content: String) {
        show(project, title, content, notificationGroup, NotificationType.WARNING, null)
    }

    /**
     * Shows {@link Notification} in ".ignore plugin" group.
     *
     * @param project   current project
     * @param title     notification title
     * @param content   notification text
     */
    fun showError(project: Project, title: String, content: String) {
        show(project, title, content, notificationGroup, NotificationType.ERROR, null)
    }

    /**
     * Shows {@link Notification} in ".ignore plugin" group.
     *
     * @param project   current project
     * @param title     notification title
     * @param content   notification text
     */
    fun showInfo(project: Project, title: String, content: String) {
        show(project, title, content, notificationGroup, NotificationType.INFORMATION, null)
    }

    /**
     * Shows {@link Notification} in ".ignore plugin" group.
     *
     * @param project   current project
     * @param title     notification title
     * @param content   notification text
     * @param type      notification type
     * @param listener  optional listener
     */
    fun show(
        project: Project, title: String, content: String,
        type: NotificationType, listener: NotificationListener?,
    ) {
        show(project, title, content, notificationGroup, type, listener)
    }

    /**
     * Shows {@link Notification}.
     *
     * @param project  current project
     * @param title    notification title
     * @param group    notification group
     * @param content  notification text
     * @param type     notification type
     * @param listener optional listener
     */
    fun show(
        project: Project,
        title: String,
        content: String,
        group: NotificationGroup,
        type: NotificationType,
        listener: NotificationListener?,
    ) {
        val notification: Notification = group.createNotification(title, content, type, listener)
        Notifications.Bus.notify(notification, project)
    }

    fun create(type: NotificationType, project: Project, title: String? = null, content: String): CaosNotification {
        return CaosNotification(
            notificationGroup = notificationGroup,
            type = type,
            title = title,
            content = content
        ) { notification ->
            Notifications.Bus.notify(notification, project)
        }
    }

    fun createErrorNotification(project: Project, title: String? = null, content: String): CaosNotification {
        return create(NotificationType.ERROR, project, title, content)
    }


    fun createWarningNotification(project: Project, title: String? = null, content: String): CaosNotification {
        return create(NotificationType.ERROR, project, title, content)
    }

    fun createInfoNotification(project: Project, title: String? = null, content: String): CaosNotification {
        return create(NotificationType.INFORMATION, project, title, content)
    }

}

data class CaosNotification internal constructor(
    private val notificationGroup: NotificationGroup,
    val type: NotificationType,
    val title: String? = null,
    val content: String? = null,
    val actions: List<AnAction> = emptyList(),
    val icon: Icon? = null,
    val subtitle: String? = null,
    val dropDownText: String? = null,
    val contextHelpAction: AnAction? = null,
    val important: Boolean? = null,
    val expired: Boolean? = null,
    val listener: NotificationListener? = null,
    // In charge of actually showing the notification
    private val _show: (notification: Notification) -> Unit,
) {

    fun setGroup(group: NotificationGroup): CaosNotification {
        return copy(
            notificationGroup = group
        )
    }

    fun addAction(action: AnAction): CaosNotification {
        return this.copy(
            actions = actions + action
        )
    }

    fun addAction(text: String, action: (e: AnActionEvent) -> Unit): CaosNotification {
        return addAction(object: AnAction(text) {
            override fun actionPerformed(e: AnActionEvent) {
                action(e)
            }
        })
    }

    fun addAction(
        text: String,
        description: String,
        icon: Icon? = null,
        action: (e: AnActionEvent) -> Unit,
    ): CaosNotification {
        return addAction(object: AnAction(text, description, icon) {
            override fun actionPerformed(e: AnActionEvent) {
                action(e)
            }
        })
    }

    fun addActions(vararg actions: AnAction): CaosNotification {
        return this.copy(
            actions = this.actions + actions
        )
    }

    fun setIcon(icon: Icon): CaosNotification {
        return copy(
            icon = icon
        )
    }

    fun setSubtitle(subtitle: String?): CaosNotification {
        return copy(
            subtitle = subtitle
        )
    }

    fun setDropDownText(text: String?): CaosNotification {
        return copy(
            dropDownText = text
        )
    }

    fun setContextHelpAction(value: AnAction): CaosNotification {
        return copy(
            contextHelpAction = value
        )
    }

    fun expired(expired: Boolean): CaosNotification {
        return copy(
            expired = expired
        )
    }

    fun setImportant(important: Boolean): CaosNotification {
        return copy(
            important = important
        )
    }

    fun setListener(listener: NotificationListener): CaosNotification {
        return copy(
            listener = listener
        )
    }

    fun setTitle(title: String): CaosNotification {
        return copy(
            title = title
        )
    }


    fun setContent(content: String): CaosNotification {
        return copy(
            content = content
        )
    }

    fun show(): Notification {
        var notification = notificationGroup.createNotification(type)
        title?.nullIfEmpty()?.let { title ->
            notification = notification.setTitle(title)
        }
        content?.nullIfEmpty()?.let { content ->
            notification = notification.setContent(content)
        }
        for (action in actions) {
            notification = notification.addAction(action)
        }
        icon?.let { icon ->
            notification = notification.setIcon(icon)
        }
        subtitle?.let { subtitle ->
            notification = notification.setSubtitle(subtitle)
        }
        dropDownText?.let { dropDownText ->
            notification = notification.setDropDownText(dropDownText)
        }
        contextHelpAction?.let { action ->
            notification = notification.setContextHelpAction(action)
        }
        important?.let { important ->
            notification = notification.setImportant(important)
        }
        if (expired == true) {
            notification.expire()
        }
        listener?.let { listener ->
            notification = notification.setListener(listener)
        }
        _show(notification)
        return notification
    }
}

object CaosInjectorNotifications : CaosNotificationsBase("creatures.CAOS_INJECTOR")
object CaosNotifications : CaosNotificationsBase("creatures.CAOS_MAIN")
object CaosBalloonNotifications : CaosNotificationsBase("creatures.CAOS_BALLOON")