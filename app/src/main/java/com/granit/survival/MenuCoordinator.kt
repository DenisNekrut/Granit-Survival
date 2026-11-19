package com.granit.survival

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.widget.LinearLayout
import android.widget.Button
import android.widget.Toast

/** Owns main-menu decisions; MainActivity remains responsible for Android lifecycle. */
internal class MenuCoordinator(
    private val activity: Activity,
    private val createPanel: (String, String) -> LinearLayout,
    private val menuButton: (String, Boolean, () -> Unit) -> Button,
    private val showOverlay: (LinearLayout) -> Unit,
    private val hasSavedGame: () -> Boolean,
    private val startNewGame: () -> Unit,
    private val loadMostRecent: () -> Boolean,
    private val loadError: () -> String?,
    private val enterGame: () -> Unit,
    private val showSaveSlots: () -> Unit,
    private val showSettings: () -> Unit,
    private val exit: () -> Unit
) {
    fun showMainMenu() {
        val panel = createPanel("GRANIT SURVIVAL", "Тишина — тоже часть выживания")
        panel.addView(menuButton("НОВАЯ ИГРА", true) {
            if (hasSavedGame()) {
                AlertDialog.Builder(activity)
                    .setTitle("Начать новую игру?")
                    .setMessage("Текущие сохранения останутся в слотах, но начнётся новая игровая сессия.")
                    .setNegativeButton("ОТМЕНА", null)
                    .setPositiveButton("НАЧАТЬ") { _, _ -> startNewGame() }
                    .show()
            } else startNewGame()
        })
        panel.addView(menuButton("ПРОДОЛЖИТЬ", hasSavedGame()) {
            if (loadMostRecent()) enterGame()
            else Toast.makeText(activity, loadError() ?: "Нет доступного сохранения", Toast.LENGTH_LONG).show()
        })
        panel.addView(menuButton("СОХРАНЕНИЯ", true) { showSaveSlots() })
        panel.addView(menuButton("НАСТРОЙКИ", true) { showSettings() })
        panel.addView(menuButton("ОБНОВИТЬ", true) { openLatestUpdate() })
        panel.addView(menuButton("ВЫХОД", true) { exit() })
        showOverlay(panel)
    }

    private fun openLatestUpdate() {
        val releasesUrl = "https://github.com/DenisNekrut/Granit-Survival/releases/latest"
        runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(releasesUrl))) }
            .onFailure {
                Toast.makeText(activity, "Не удалось открыть страницу обновлений", Toast.LENGTH_LONG).show()
            }
    }
}
