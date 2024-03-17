package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.*

@Entity
@Table(name = "category_table")
data class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    /** суффикс (для команд типа /toggle_{suffix}) **/
    @Column(name = "suffix", columnDefinition = "VARCHAR(64)")
    val suffix: String? = null,
    /** название категории **/
    @Column(name = "title", columnDefinition = "VARCHAR(64)")
    val title: String? = null,
    /** описание категории **/
    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,
    /** последний редактор категории **/
    @Column(name = "changed_by_tui")
    val changedByTui: String? = null,
    /** возможно ли снять категорию **/
    @Column(name = "is_unremovable")
    val isUnremovable: Boolean? = null,
    /** установлена ли у пользователей по умолчанию **/
    @Column(name = "is_setup_by_default")
    val isSetupByDefault: Boolean = true,
)
