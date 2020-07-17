package insulator.views.main.schemaregistry

import insulator.Styles
import insulator.viewmodel.main.schemaregistry.ListSchemaViewModel
import insulator.viewmodel.main.topic.TopicViewModel
import insulator.views.main.topic.TopicView
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.SelectionMode
import tornadofx.*

class ListSchemaView : View() {

    private val viewModel: ListSchemaViewModel by inject()
    private val searchItem = SimpleStringProperty()

    override val root = vbox {
        label("Schema registry") { addClass(Styles.h1, Styles.mainColor) }
        hbox { label("Search"); textfield(searchItem) { minWidth = 200.0 }; alignment = Pos.CENTER_RIGHT; spacing = 5.0 }
        listview<String> {
            cellFormat {
                graphic = label(it)
            }
            onDoubleClick {
                if (this.selectedItem == null) return@onDoubleClick
                val scope = Scope()
//                tornadofx.setInScope(this.selectedItem!!, scope)
//                find<TopicView>(scope).openWindow()
            }
            runAsync {
                itemsProperty().set(
                        SortedFilteredList(viewModel.listSchemas()).apply {
                            filterWhen(searchItem) { p, i -> i.contains(p) }
                        }.filteredItems
                )
            }

            selectionModel.selectionMode = SelectionMode.SINGLE
            prefHeight = 600.0 //todo: remove hardcoded and retrieve
        }
        addClass(Styles.card)
    }
}