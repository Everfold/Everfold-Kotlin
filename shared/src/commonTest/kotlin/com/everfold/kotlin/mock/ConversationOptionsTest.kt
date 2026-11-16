package com.everfold.kotlin.mock

import androidx.lifecycle.ViewModelStore
import com.everfold.kotlin.feature.connections.ClusterSetup
import com.everfold.kotlin.feature.conversation.AttachmentSource
import com.everfold.kotlin.feature.conversation.AttachmentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationOptionsTest {
    @Test
    fun firstSendBindsWorkspaceAndRecordsModelsWithoutRewritingHistory() =
        optionsTest { app ->
            val options = app.conversationOptions
            val draft = app.newConversation
            val project = options.workspaces(draft.connectionId).last()
            val model = options.models(draft.connectionId).last()
            options.selectWorkspace(draft.id, project.id)
            options.selectModel(draft.id, model.id)
            app.updateNewConversationDraft("First")
            val id = assertNotNull(app.submitNewConversation())
            options.selectWorkspace(id, options.workspaces(draft.connectionId).first().id)
            options.selectModel(id, options.models(draft.connectionId).first().id)
            assertEquals(model, app.conversations.first { it.id == id }.model)
            advanceUntilIdle()
            options.selectModel(id, options.models(draft.connectionId).first().id)
            app.updateDraft(id, "Second")
            app.send(id)
            advanceUntilIdle()
            val session = app.conversations.first { it.id == id }
            assertEquals(project, session.workspace)
            assertEquals(model, session.messages.first().model)
            assertEquals(model, session.messages[1].model)
            assertEquals(session.model, session.messages.last().model)
        }

    @Test
    fun attachmentOnlySendWaitsForUploadAndKeepsTheSubmittedSnapshot() =
        optionsTest { app ->
            val target = app.newConversation.attachmentTarget
            app.conversationOptions.attach(target, listOf(AttachmentSource("report.pdf", "file:///report.pdf")))
            assertFalse(app.newConversation.hasSendableContent)
            assertNull(app.submitNewConversation())
            advanceUntilIdle()
            assertTrue(app.newConversation.hasSendableContent)
            val id = assertNotNull(app.submitNewConversation())
            assertTrue(app.newConversation.attachments.isEmpty())
            val message =
                app.conversations
                    .first { it.id == id }
                    .messages
                    .first()
            assertEquals(
                "report.pdf",
                message.attachments
                    .single()
                    .source.name,
            )
            assertEquals(AttachmentStatus.Ready, message.attachments.single().status)
            app.conversationOptions.attach(target, listOf(AttachmentSource("late.txt", "file:///late.txt")))
            assertTrue(app.newConversation.attachments.isEmpty())
            advanceUntilIdle()
        }

    @Test
    fun changingTargetsClearsAttachmentsAndRejectsLatePickerResultsButKeepsText() =
        optionsTest { app ->
            app.updateNewConversationDraft("Keep text")
            val options = app.conversationOptions
            val oldTarget = app.newConversation.attachmentTarget
            options.attach(oldTarget, listOf(AttachmentSource("old.txt", "file:///old.txt")))
            val project = options.workspaces(oldTarget.connectionId).last()
            options.selectWorkspace(oldTarget.conversationId, project.id)
            options.attach(oldTarget, listOf(AttachmentSource("late.txt", "file:///late.txt")))
            assertTrue(app.newConversation.attachments.isEmpty())
            val projectTarget = app.newConversation.attachmentTarget
            options.attach(projectTarget, listOf(AttachmentSource("project.txt", "file:///project.txt")))
            val remote = assertNotNull(app.connections.add("Office", ClusterSetup()))
            app.selectNewConversationConnection(remote)
            options.attach(projectTarget, listOf(AttachmentSource("stale.txt", "file:///stale.txt")))
            options.selectWorkspace(app.newConversation.id, project.id)
            options.selectModel(app.newConversation.id, options.models(oldTarget.connectionId).first().id)
            advanceUntilIdle()
            assertTrue(app.newConversation.attachments.isEmpty())
            assertEquals("Keep text", app.newConversation.draft)
            assertEquals(options.workspaces(remote).first(), app.newConversation.workspace)
            assertEquals(options.models(remote).first(), app.newConversation.model)
        }

    @Test
    fun interruptedUploadsCanRetryAndRemovedUploadsCannotReappear() =
        optionsTest { app ->
            val target = app.newConversation.attachmentTarget
            val options = app.conversationOptions
            options.attach(target, listOf(AttachmentSource("report.pdf", "file:///report.pdf")))
            val attachment = app.newConversation.attachments.single()
            app.connections.disconnect(target.connectionId)
            advanceUntilIdle()
            assertEquals(
                AttachmentStatus.Failed,
                app.newConversation.attachments
                    .single()
                    .status,
            )
            app.connections.connect(target.connectionId)
            advanceUntilIdle()
            options.retryAttachment(target.conversationId, attachment.id)
            advanceUntilIdle()
            assertEquals(
                AttachmentStatus.Ready,
                app.newConversation.attachments
                    .single()
                    .status,
            )
            options.attach(target, listOf(AttachmentSource("next.pdf", "file:///next.pdf")))
            options.removeAttachment(
                target.conversationId,
                app.newConversation.attachments
                    .last()
                    .id,
            )
            advanceUntilIdle()
            assertEquals(listOf(attachment.id), app.newConversation.attachments.map { it.id })
        }

    @Test
    fun deletingOneSessionCancelsItsUploadWithoutTouchingAnotherDraft() =
        optionsTest { app ->
            val session = app.conversations.first()
            val files = listOf(AttachmentSource("report.pdf", "file:///report.pdf"))
            app.conversationOptions.attach(session.attachmentTarget, files)
            app.conversationOptions.attach(app.newConversation.attachmentTarget, files)
            app.deleteConversation(session.id)
            app.conversationOptions.attach(session.attachmentTarget, files)
            advanceUntilIdle()
            assertTrue(app.conversations.none { it.id == session.id })
            assertEquals(
                AttachmentStatus.Ready,
                app.newConversation.attachments
                    .single()
                    .status,
            )
        }

    private fun optionsTest(block: suspend TestScope.(MockWorkspace) -> Unit) =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val store = ViewModelStore()
            try {
                val app = MockWorkspace()
                store.put("workspace", app)
                block(app)
            } finally {
                store.clear()
                Dispatchers.resetMain()
            }
        }
}
