export interface Vector3State {
  x: number
  y: number
  z: number
}

export interface OrientationState {
  /** 航向角，单位：度，0° 指向正北（Z 正方向） */
  yaw: number
  /** 俯仰角，单位：度，正值表示抬头 */
  pitch: number
  /** 横滚角，单位：度，正值表示向右倾斜 */
  roll: number
}

export interface VelocityState {
  /** 前向速度，单位：m/s */
  forward: number
  /** 侧向速度（右正左负），单位：m/s */
  strafe: number
  /** 垂向速度（上负下正），单位：m/s */
  vertical: number
}

export interface ThrusterState {
  /** 前向推进器输出占比，范围 -100 ~ 100 */
  forward: number
  /** 侧向推进器输出占比，范围 -100 ~ 100 */
  strafe: number
  /** 垂向推进器输出占比，范围 -100 ~ 100 */
  vertical: number
  /** 航向调整推进器输出占比，范围 -100 ~ 100 */
  yaw: number
}

export interface SensorSnapshot {
  depth: number
  temperature: number
  heading: number
  pitch: number
  roll: number
  battery: number
}

export interface RobotState {
  position: Vector3State
  orientation: OrientationState
  velocity: VelocityState
  thrusters: ThrusterState
  sensors: SensorSnapshot
  timestamp: number
}

export interface SceneRobotConfig {
  id: string
  name: string
  accent: string
  body: string
  trim: string
  initialState: RobotState
}

export interface SceneRobot extends SceneRobotConfig {
  state: RobotState
}
