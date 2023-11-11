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
    @Column(name = "suffix")
    val suffix: String? = null,

    /** название категории **/
    @Column(name = "suffix")
    val title: String? = null,

    /** описание категории **/
    @Column(name = "description")
    val description: String? = null,

    /** установлена ли у пользователей по умолчанию **/
    @Column(name = "is_setup_by_default")
    val isSetupByDefault: Boolean = true,
)
