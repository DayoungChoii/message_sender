package com.mfort.momsitter.domain

import jakarta.persistence.*
import jakarta.persistence.GenerationType.IDENTITY

@Entity
@Table(name = "message")
class Message(
    @Column(name = "phone_number", length = 20, nullable = false)
    val phoneNumber: String,

    @Column(length = 100, nullable = false)
    val title: String,

    @Column(length = 500, nullable = false)
    val contents: String,
): BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    val id: Long? = null
}