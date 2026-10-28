import {
  authenticate, clearFailedLogins, createSession, deleteSession, lockedForMs, recordFailedLogin, verifyPassword,
} from "./auth.js"
import { resetDatabase, seededAt } from "./db.js"
import {
  HttpError, bootstrap, getAssignment, getCourse, gradeReportCsv, learningSummary, listAssignments,
  listCourses, listNotifications, markCompleted, markNotificationRead, profileOf, semesterOverview,
  timetable, unmarkCompleted,
} from "./services.js"

const MAX_BODY_BYTES = 64 * 1024

function makeRoutes(allowReset) {
  const routes = [
    {
      method: "GET", path: "/health", auth: false,
      run(ctx) {
        return { status: "ok", serverTime: ctx.now, seededAt: seededAt(ctx.db) }
      },
    },
    {
      method: "POST", path: "/api/v1/auth/login", auth: false,
      run(ctx) {
        return login(ctx)
      },
    },
    {
      method: "POST", path: "/api/v1/auth/logout", auth: true,
      run(ctx) {
        deleteSession(ctx.db, ctx.user.token)
        return { ok: true }
      },
    },
    {
      method: "GET", path: "/api/v1/me", auth: true,
      run(ctx) {
        return profileOf(ctx.db, ctx.user)
      },
    },
    {
      method: "GET", path: "/api/v1/bootstrap", auth: true,
      run(ctx) {
        return bootstrap(ctx.db, ctx.user, ctx.now)
      },
    },
    {
      method: "GET", path: "/api/v1/summary", auth: true,
      run(ctx) {
        return learningSummary(ctx.db, ctx.user.id, ctx.now)
      },
    },
    {
      method: "GET", path: "/api/v1/semester", auth: true,
      run(ctx) {
        return semesterOverview(ctx.db, ctx.user, ctx.now)
      },
    },
    {
      method: "GET", path: "/api/v1/courses", auth: true,
      run(ctx) {
        return { items: listCourses(ctx.db, ctx.user.id, ctx.now) }
      },
    },
    {
      method: "GET", path: "/api/v1/courses/:id", auth: true,
      run(ctx) {
        return getCourse(ctx.db, ctx.user.id, ctx.params.id, ctx.now)
      },
    },
    {
      method: "GET", path: "/api/v1/assignments", auth: true,
      run(ctx) {
        const options = {
          courseId: ctx.query.get("courseId") || undefined,
          filter: ctx.query.get("filter") || undefined,
        }
        return { items: listAssignments(ctx.db, ctx.user.id, ctx.now, options) }
      },
    },
    {
      method: "GET", path: "/api/v1/assignments/:id", auth: true,
      run(ctx) {
        return getAssignment(ctx.db, ctx.user.id, ctx.params.id, ctx.now)
      },
    },
    {
      method: "POST", path: "/api/v1/assignments/:id/completion", auth: true,
      run(ctx) {
        return markCompleted(ctx.db, ctx.user.id, ctx.params.id, ctx.now)
      },
    },
    {
      method: "DELETE", path: "/api/v1/assignments/:id/completion", auth: true,
      run(ctx) {
        return unmarkCompleted(ctx.db, ctx.user.id, ctx.params.id, ctx.now)
      },
    },
    {
      method: "GET", path: "/api/v1/timetable", auth: true,
      run(ctx) {
        return timetable(ctx.db, ctx.user.id)
      },
    },
    {
      method: "GET", path: "/api/v1/notifications", auth: true,
      run(ctx) {
        return listNotifications(ctx.db, ctx.user.id)
      },
    },
    {
      method: "POST", path: "/api/v1/notifications/:id/read", auth: true,
      run(ctx) {
        return markNotificationRead(ctx.db, ctx.user.id, ctx.params.id, ctx.now)
      },
    },
    {
      method: "GET", path: "/api/v1/reports/grades.csv", auth: true,
      run(ctx) {
        const profile = profileOf(ctx.db, ctx.user)
        // VD: "USTH-Semester-1-2026-2027-grade-report.csv" (đổi khoảng trắng và dấu – thành "-")
        let filename = "USTH-" + profile.semester + "-" + profile.academicYear + "-grade-report.csv"
        filename = filename.replaceAll(" ", "-").replaceAll("–", "-")
        return {
          status: 200,
          headers: {
            "Content-Type": "text/csv; charset=utf-8",
            "Content-Disposition": 'attachment; filename="' + filename + '"',
          },
          body: gradeReportCsv(ctx.db, ctx.user, ctx.now),
        }
      },
    },
  ]

  
  if (allowReset) {
    routes.push({
      method: "POST", path: "/api/v1/dev/reset", auth: false,
      run(ctx) {
        resetDatabase(ctx.db, ctx.now)
        return { ok: true, seededAt: ctx.now }
      },
    })
  }
  return routes
}

function login(ctx) {
  const body = ctx.body || {}
  const username = String(body.username || "").trim().toLowerCase()
  const password = String(body.password || "")
  if (username === "" || password === "") {
    throw new HttpError(400, "missing_credentials", "Vui lòng nhập mã sinh viên và mật khẩu")
  }

  
  const waitMs = lockedForMs(ctx.failedLogins, username, ctx.now)
  if (waitMs > 0) {
    const minutes = Math.ceil(waitMs / 60000)
    const headers = { "Retry-After": String(Math.ceil(waitMs / 1000)) }
    throw new HttpError(429, "too_many_attempts", "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau " + minutes + " phút.", headers)
  }

  const user = ctx.db.prepare("SELECT * FROM users WHERE username = ?").get(username)
  if (!user || !verifyPassword(password, user.password_hash)) {
    recordFailedLogin(ctx.failedLogins, username, ctx.now)
    throw new HttpError(401, "invalid_credentials", "Mã sinh viên hoặc mật khẩu không đúng")
  }

  clearFailedLogins(ctx.failedLogins, username)
  const session = createSession(ctx.db, user.id, ctx.now)
  return { token: session.token, expiresAt: session.expiresAt, profile: profileOf(ctx.db, user) }
}

