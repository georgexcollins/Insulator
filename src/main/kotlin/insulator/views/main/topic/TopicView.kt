package insulator.views.main.topic

import insulator.lib.configuration.model.Cluster
import insulator.lib.kafka.ConsumeFrom
import insulator.lib.kafka.DeserializationFormat
import insulator.styles.Controls
import insulator.styles.Titles
import insulator.viewmodel.main.topic.RecordViewModel
import insulator.viewmodel.main.topic.TopicViewModel
import insulator.views.common.searchBox
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableView
import javafx.scene.input.Clipboard
import javafx.scene.layout.Priority
import tornadofx.* // ktlint-disable no-wildcard-imports
import java.util.concurrent.Callable

class TopicView : View() {

    private val cluster: Cluster by di()
    private val viewModel: TopicViewModel by inject()
    private val searchItem = SimpleStringProperty()
    private val messageConsumedCountProperty: ObservableValue<Number> = Bindings.size(viewModel.records)
    private val subtitleProperty = Bindings.createStringBinding(
        Callable {
            "Message count: ${messageConsumedCountProperty.value}/${viewModel.messageCountProperty.value} - " +
                "Is internal: ${viewModel.isInternalProperty.value} - " +
                "Partitions count: ${viewModel.partitionCountProperty.value} - " +
                "Compacted: ${viewModel.isCompactedProperty.value}"
        },
        messageConsumedCountProperty,
        viewModel.isCompactedProperty,
        viewModel.messageCountProperty
    )

    override val root = borderpane {
        top = vbox {
            vbox {
                hbox(spacing = 10.0, alignment = Pos.CENTER_LEFT) {
                    label(viewModel.nameProperty.value) { addClass(Titles.h1) }
                    deleteButton()
                }
                label(subtitleProperty) { addClass(Titles.h3) }
                addClass(Controls.topBarMenu, Titles.subtitle)
            }
            hbox { addClass(Controls.topBarMenuShadow) }
        }
        center = vbox(spacing = 2.0) {
            borderpane {
                left = hbox(alignment = Pos.CENTER, spacing = 5.0) {
                    button(viewModel.consumeButtonText) { action { viewModel.consume() }; prefWidth = 80.0 }
                    label("from")
                    combobox<String> {
                        items = FXCollections.observableArrayList(ConsumeFrom.values().map { it.name }.toList())
                        valueProperty().bindBidirectional(viewModel.consumeFromProperty)
                    }
                    if (cluster.isSchemaRegistryConfigured()) {
                        label("value format")
                        viewModel.deserializeValueProperty.value = DeserializationFormat.Avro.toString()
                        combobox<String> {
                            items = FXCollections.observableArrayList(DeserializationFormat.values().map { it.name }.toList())
                            valueProperty().bindBidirectional(viewModel.deserializeValueProperty)
                        }
                    }
                    region { minWidth = 10.0 }
                    button("Clear") { action { viewModel.clear() } }
                }
                right = searchBox(searchItem)
            }
            tableview<RecordViewModel> {
                column("Time", RecordViewModel::timestampProperty) {
                    prefWidthProperty().bind(this.tableView.widthProperty().divide(4).multiply(1))
                }
                column("Key", RecordViewModel::keyProperty) {
                    prefWidthProperty().bind(this.tableView.widthProperty().divide(4).multiply(1))
                }
                column("Value", RecordViewModel::valueProperty) {
                    prefWidthProperty().bind(this.tableView.widthProperty().divide(4).multiply(2).minus(4.0))
                    enableTextWrap()
                }
                itemsProperty().set(recordList())
                contextMenu = contextmenu {
                    item("Copy") {
                        action {
                            if (selectedItem !is RecordViewModel) return@action
                            Clipboard.getSystemClipboard().putString(selectedItem!!.toCsv())
                        }
                    }
                    item("Copy all") {
                        action {
                            Clipboard.getSystemClipboard().putString(recordList().joinToString("\n") { it.toCsv() })
                        }
                    }
                }
                selectionModel.selectionMode = SelectionMode.SINGLE
                vgrow = Priority.ALWAYS
            }
        }
        addClass(Controls.view)
    }

    private fun EventTarget.deleteButton() = apply {
        button("Delete") {
            addClass(Controls.alertButton)
            action {
                val closeWindow = { close() }
                alert(
                    Alert.AlertType.WARNING,
                    "The topic \"${viewModel.nameProperty.value}\" will be removed.", null,
                    ButtonType.CANCEL, ButtonType.OK,
                    owner = currentWindow,
                    actionFn = { buttonType ->
                        when (buttonType) {
                            ButtonType.OK -> {
                                viewModel.delete()
                                closeWindow()
                            }
                            else -> Unit
                        }
                    }
                )
            }
        }
    }

    private fun TableView<RecordViewModel>.recordList() =
        SortedFilteredList(viewModel.records).apply {
            filterWhen(searchItem) { p, i ->
                i.keyProperty.value?.toLowerCase()?.contains(p.toLowerCase()) ?: false ||
                    i.valueProperty.value.toLowerCase().contains(p.toLowerCase())
            }
        }.sortedItems.also {
            it.comparatorProperty().bind(this.comparatorProperty())
        }

    private fun RecordViewModel.toCsv() = "${this.timestampProperty.value}\t" +
        "${this.keyProperty.value}\t" +
        this.valueProperty.value

    override fun onDock() {
        currentWindow?.setOnCloseRequest { viewModel.stop() }
        titleProperty.bind(viewModel.nameProperty)
        super.currentStage?.width = 800.0
        super.currentStage?.height = 800.0
        super.onDock()
    }
}
