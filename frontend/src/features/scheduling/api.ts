import { http } from '../../api/http'
import type { MySchedule, ScheduleBoard } from '../../types'

export const scheduleBoardApi = {
  get: (from: string, to: string) =>
    http.get<ScheduleBoard>('/schedule-board', { params: { from, to } }).then((response) => response.data),
  getMine: (from: string, to: string) =>
    http.get<MySchedule>('/my-schedule', { params: { from, to } }).then((response) => response.data),
}
