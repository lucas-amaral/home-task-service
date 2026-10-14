package com.amaral.hometask.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Turns the @Scheduled jobs on. Without it they are never run: the penalty and notification
 * schedulers existed but had never fired.
 *
 * Render's free plan sleeps when idle, so every job here also has an endpoint (see CronController)
 * that an external cron calls; the jobs are safe to run twice.
 *
 * @Lazy(false), like the schedulers themselves: the app boots with lazy-initialization, and a
 * lazy @Scheduled bean is never created, so its job would silently never run.
 */
@Configuration
@EnableScheduling
@Lazy(false)
class SchedulingConfig
