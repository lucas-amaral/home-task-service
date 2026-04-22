package com.amaral.hometask.controller

import com.amaral.hometask.service.BoardService
import com.amaral.hometask.util.DateTimeUtils
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api")
class BoardController(
    private val boardService: BoardService
) {

    @GetMapping("/health")
    fun health() = mapOf(
        "status" to "ok",
        "today" to DateTimeUtils.today(),
        "weekStart" to DateTimeUtils.weekStart()
    )

    @GetMapping("/board")
    fun getBoard(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?
    ) = boardService.getBoard(date ?: DateTimeUtils.today())
}

