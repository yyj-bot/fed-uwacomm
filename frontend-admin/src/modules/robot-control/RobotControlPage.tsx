import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Card, Typography, Tag, Divider, Space, Button, message } from 'antd'
import {
  RocketOutlined,
  AimOutlined,
  ThunderboltOutlined,
  CompassOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import { MathUtils } from 'three'

import RobotScene from './RobotScene'
import styles from './RobotControlPage.module.css'
import type { RobotState, ThrusterState, SceneRobotConfig, SceneRobot } from './types'

const { Title, Text } = Typography

interface ControlState {
  [key: string]: boolean
}

const INITIAL_STATE: RobotState = {
  position: { x: 0, y: -5, z: 0 },
  orientation: { yaw: 45, pitch: 0, roll: 0 },
  velocity: { forward: 0, strafe: 0, vertical: 0 },
  thrusters: { forward: 0, strafe: 0, vertical: 0, yaw: 0 },
  sensors: {
    depth: 5,
    temperature: 19.4,
    heading: 45,
    pitch: 0,
    roll: 0,
    battery: 92
  },
  timestamp: Date.now()
}

type PartialRobotState = {
  position?: Partial<RobotState['position']>
  orientation?: Partial<RobotState['orientation']>
  velocity?: Partial<RobotState['velocity']>
  thrusters?: Partial<RobotState['thrusters']>
  sensors?: Partial<RobotState['sensors']>
  timestamp?: number
}

const cloneRobotState = (state: RobotState, overrides?: PartialRobotState): RobotState => {
  return {
    position: { ...state.position, ...(overrides?.position ?? {}) },
    orientation: { ...state.orientation, ...(overrides?.orientation ?? {}) },
    velocity: { ...state.velocity, ...(overrides?.velocity ?? {}) },
    thrusters: { ...state.thrusters, ...(overrides?.thrusters ?? {}) },
    sensors: { ...state.sensors, ...(overrides?.sensors ?? {}) },
    timestamp: overrides?.timestamp ?? Date.now()
  }
}

const ROBOT_CONFIGS: SceneRobotConfig[] = [
  {
    id: 'auv-01',
    name: 'AUV-01',
    accent: '#f7cf3a',
    body: '#4f565f',
    trim: '#626b75',
    initialState: cloneRobotState(INITIAL_STATE, {
      position: { x: 0, y: -5, z: 0 },
      orientation: { yaw: 40 },
      sensors: { battery: 92 }
    })
  },
  {
    id: 'auv-02',
    name: 'AUV-02',
    accent: '#40c4ff',
    body: '#465260',
    trim: '#5f6c79',
    initialState: cloneRobotState(INITIAL_STATE, {
      position: { x: -7, y: -5.2, z: 4 },
      orientation: { yaw: 120 },
      sensors: { battery: 86 }
    })
  },
  {
    id: 'auv-03',
    name: 'AUV-03',
    accent: '#ff9e3d',
    body: '#515a63',
    trim: '#6b757f',
    initialState: cloneRobotState(INITIAL_STATE, {
      position: { x: 6.5, y: -5.4, z: -3.5 },
      orientation: { yaw: 300 },
      sensors: { battery: 78 }
    })
  },
  {
    id: 'auv-04',
    name: 'AUV-04',
    accent: '#7bdd91',
    body: '#43515a',
    trim: '#5a6770',
    initialState: cloneRobotState(INITIAL_STATE, {
      position: { x: -3.5, y: -4.7, z: -8 },
      orientation: { yaw: 210 },
      sensors: { battery: 68 }
    })
  },
  {
    id: 'auv-05',
    name: 'AUV-05',
    accent: '#d480ff',
    body: '#4a5561',
    trim: '#606a77',
    initialState: cloneRobotState(INITIAL_STATE, {
      position: { x: 4.2, y: -5.3, z: 7 },
      orientation: { yaw: 30 },
      sensors: { battery: 58 }
    })
  }
]

const CONTROL_KEYS = new Set([
  'w', 'a', 's', 'd',
  'arrowup', 'arrowdown', 'arrowleft', 'arrowright',
  'q', 'e', ' ', 'space', 'shift',
  'j', 'l'
])

const MAX_FORWARD_SPEED = 1.6
const MAX_STRAFE_SPEED = 1.1
const MAX_VERTICAL_SPEED = 1.0
const MAX_YAW_RATE = 65 // deg/s
const POOL_LIMIT = 28
const CEILING_LEVEL = -0.4
const FLOOR_LEVEL = -14.5

const MAX_FORWARD_THRUST = 100
const MAX_STRAFE_THRUST = 90
const MAX_VERTICAL_THRUST = 85
const MAX_YAW_THRUST = 80
const ENCRYPTION_RADIUS = 6.5

const POSITION_PRECISION = 3
const VELOCITY_PRECISION = 3

const sanitizeAngle = (value: number) => {
  let angle = value % 360
  if (angle < 0) angle += 360
  return angle
}

const approachValue = (current: number, target: number, maxDelta: number) => {
  if (current === target) return current
  const delta = target - current
  const step = Math.sign(delta) * Math.min(Math.abs(delta), maxDelta)
  const next = current + step
  return Math.abs(next) < 0.5 ? 0 : MathUtils.clamp(next, -100, 100)
}

const computeSensors = (
  positionY: number,
  orientation: RobotState['orientation'],
  previousBattery: number
) => {
  const depth = Math.max(0, -(positionY))
  const heading = sanitizeAngle(orientation.yaw)
  const temperature = Math.max(2.5, 23 - depth * 0.32 + Math.sin((heading / 360) * Math.PI * 2) * 0.35)
  const battery = MathUtils.clamp(previousBattery, 0, 100)

  return {
    depth: parseFloat(depth.toFixed(2)),
    temperature: parseFloat(temperature.toFixed(1)),
    heading: parseFloat(heading.toFixed(1)),
    pitch: parseFloat(orientation.pitch.toFixed(1)),
    roll: parseFloat(orientation.roll.toFixed(1)),
    battery: parseFloat(battery.toFixed(1))
  }
}

const updateRobotState = (
  prev: RobotState,
  controls: ControlState,
  dt: number
): RobotState => {
  const isPressed = (key: string) => controls[key] === true

  const forwardActive = isPressed('w') || isPressed('arrowup')
  const backwardActive = isPressed('s') || isPressed('arrowdown')
  const leftActive = isPressed('a') || isPressed('arrowleft')
  const rightActive = isPressed('d') || isPressed('arrowright')
  const ascendActive = isPressed('q') || isPressed(' ') || isPressed('space')
  const descendActive = isPressed('e') || isPressed('shift')
  const yawLeftActive = isPressed('j')
  const yawRightActive = isPressed('l')
  const verticalControlActive = ascendActive || descendActive

  const targetThrusters: ThrusterState = {
    forward: 0,
    strafe: 0,
    vertical: 0,
    yaw: 0
  }

  if (forwardActive) targetThrusters.forward += 100
  if (backwardActive) targetThrusters.forward -= 100
  if (rightActive) targetThrusters.strafe += 90
  if (leftActive) targetThrusters.strafe -= 90
  if (ascendActive) targetThrusters.vertical += 85
  if (descendActive) targetThrusters.vertical -= 85
  if (yawLeftActive) targetThrusters.yaw -= 80
  if (yawRightActive) targetThrusters.yaw += 80

  const maxThrusterStep = dt * 220
  const nextThrusters: ThrusterState = {
    forward: approachValue(prev.thrusters.forward, targetThrusters.forward, maxThrusterStep),
    strafe: approachValue(prev.thrusters.strafe, targetThrusters.strafe, maxThrusterStep),
    vertical: approachValue(prev.thrusters.vertical, targetThrusters.vertical, maxThrusterStep),
    yaw: approachValue(prev.thrusters.yaw, targetThrusters.yaw, maxThrusterStep)
  }

  if (!verticalControlActive) {
    nextThrusters.vertical = 0
  }

  const forwardSpeed = (nextThrusters.forward / 100) * MAX_FORWARD_SPEED
  const strafeSpeed = (nextThrusters.strafe / 100) * MAX_STRAFE_SPEED
  const verticalSpeed = verticalControlActive ? (nextThrusters.vertical / 100) * MAX_VERTICAL_SPEED : 0

  const yawDelta = (nextThrusters.yaw / 100) * MAX_YAW_RATE * dt
  const nextYaw = sanitizeAngle(prev.orientation.yaw + yawDelta)

  const targetPitch = MathUtils.clamp(-nextThrusters.forward * 0.12, -18, 18)
  const targetRoll = MathUtils.clamp(nextThrusters.strafe * -0.16, -22, 22)
  const smoothing = Math.min(1, dt * 3.2)

  const nextPitch = prev.orientation.pitch + (targetPitch - prev.orientation.pitch) * smoothing
  const nextRoll = prev.orientation.roll + (targetRoll - prev.orientation.roll) * smoothing

  const yawRad = MathUtils.degToRad(nextYaw)
  const deltaX = (forwardSpeed * Math.sin(yawRad) + strafeSpeed * Math.cos(yawRad)) * dt
  const deltaZ = (forwardSpeed * Math.cos(yawRad) - strafeSpeed * Math.sin(yawRad)) * dt
  const deltaY = verticalSpeed * dt

  let nextX = prev.position.x + deltaX
  let nextZ = prev.position.z + deltaZ
  let nextY = prev.position.y + deltaY

  nextX = MathUtils.clamp(nextX, -POOL_LIMIT, POOL_LIMIT)
  nextZ = MathUtils.clamp(nextZ, -POOL_LIMIT, POOL_LIMIT)
  nextY = MathUtils.clamp(nextY, FLOOR_LEVEL, CEILING_LEVEL)

  const nextVelocity = {
    forward: Number((forwardSpeed * 0.94).toFixed(VELOCITY_PRECISION)),
    strafe: Number((strafeSpeed * 0.94).toFixed(VELOCITY_PRECISION)),
    vertical: Number((verticalSpeed * 0.94).toFixed(VELOCITY_PRECISION))
  }

  const sensors = computeSensors(nextY, { yaw: nextYaw, pitch: nextPitch, roll: nextRoll }, prev.sensors.battery)

  return {
    position: {
      x: Number(nextX.toFixed(POSITION_PRECISION)),
      y: Number(nextY.toFixed(POSITION_PRECISION)),
      z: Number(nextZ.toFixed(POSITION_PRECISION))
    },
    orientation: {
      yaw: parseFloat(nextYaw.toFixed(1)),
      pitch: sensors.pitch,
      roll: sensors.roll
    },
    velocity: nextVelocity,
    thrusters: nextThrusters,
    sensors,
    timestamp: Date.now()
  }
}

const useRobotControl = () => {
  const [selectedId, setSelectedId] = useState<string>(ROBOT_CONFIGS[0].id)
  const [encryptionMode, setEncryptionMode] = useState<'idle' | 'pair' | 'group'>('idle')
  const [pairSelectionIds, setPairSelectionIds] = useState<string[]>([])
  const [robotStates, setRobotStates] = useState<Record<string, RobotState>>(() => {
    const initial: Record<string, RobotState> = {}
    ROBOT_CONFIGS.forEach(cfg => {
      initial[cfg.id] = cfg.initialState
    })
    return initial
  })
  const controlsRef = useRef<ControlState>({})
  const frameRef = useRef<number>()
  const lastTimeRef = useRef<number>(performance.now())

  const keyDownHandler = useCallback((event: KeyboardEvent) => {
    const key = event.key.toLowerCase()
    if (!CONTROL_KEYS.has(key)) return

    if (key === ' ' || key === 'arrowup' || key === 'arrowdown') {
      event.preventDefault()
    }
    controlsRef.current[key] = true
  }, [])

  const keyUpHandler = useCallback((event: KeyboardEvent) => {
    const key = event.key.toLowerCase()
    if (!CONTROL_KEYS.has(key)) return

    controlsRef.current[key] = false
  }, [])

  useEffect(() => {
    const tick = (now: number) => {
      const delta = Math.min((now - lastTimeRef.current) / 1000, 0.12)
      lastTimeRef.current = now

      setRobotStates(prev => {
        const current = prev[selectedId]
        if (!current) return prev
        const next = updateRobotState(current, controlsRef.current, delta)
        if (next === current) return prev
        return { ...prev, [selectedId]: next }
      })

      frameRef.current = requestAnimationFrame(tick)
    }

    frameRef.current = requestAnimationFrame(tick)

    window.addEventListener('keydown', keyDownHandler)
    window.addEventListener('keyup', keyUpHandler)

    return () => {
      window.removeEventListener('keydown', keyDownHandler)
      window.removeEventListener('keyup', keyUpHandler)
      if (frameRef.current) cancelAnimationFrame(frameRef.current)
    }
  }, [keyDownHandler, keyUpHandler, selectedId])

  useEffect(() => {
    controlsRef.current = {}
  }, [selectedId])

  const exitPairMode = useCallback((options?: { clearSelection?: boolean }) => {
    setEncryptionMode('idle')
    if (options?.clearSelection ?? true) {
      setPairSelectionIds([])
    }
  }, [])

  const reset = useCallback(() => {
    controlsRef.current = {}
    setSelectedId(ROBOT_CONFIGS[0].id)
    exitPairMode()
    setRobotStates(() => {
      const initial: Record<string, RobotState> = {}
      ROBOT_CONFIGS.forEach(cfg => {
        initial[cfg.id] = cloneRobotState(cfg.initialState)
      })
      return initial
    })
  }, [])

  const robots = useMemo(() =>
    ROBOT_CONFIGS.map(cfg => ({
      ...cfg,
      state: robotStates[cfg.id] ?? cfg.initialState
    })), [robotStates])

  const selectedRobot = useMemo(() => robots.find(robot => robot.id === selectedId), [robots, selectedId])

  const activeState = useMemo(() => {
    return selectedRobot?.state ?? ROBOT_CONFIGS[0].initialState
  }, [selectedRobot])

  const encryptionPartners = useMemo(() => {
    if (!selectedRobot) return []
    const { position } = selectedRobot.state
    return robots
      .filter(robot => robot.id !== selectedRobot.id)
      .map(robot => {
        const dx = robot.state.position.x - position.x
        const dy = robot.state.position.y - position.y
        const dz = robot.state.position.z - position.z
        const distance = Math.sqrt(dx * dx + dy * dy + dz * dz)
        return { robot, distance }
      })
      .filter(({ distance }) => distance <= ENCRYPTION_RADIUS)
      .sort((a, b) => a.distance - b.distance)
  }, [robots, selectedRobot])

  const inRangeIds = useMemo(() => encryptionPartners.map(item => item.robot.id), [encryptionPartners])

  const selectRobot = useCallback((id: string) => {
    setSelectedId(id)
  }, [])

  useEffect(() => {
    exitPairMode()
  }, [selectedId, exitPairMode])

  const enterPairMode = useCallback(() => {
    setEncryptionMode('pair')
  }, [])

  const enterGroupMode = useCallback(() => {
    setEncryptionMode('group')
  }, [])

  const togglePairCandidate = useCallback((id: string) => {
    if (!inRangeIds.includes(id) || id === selectedId) return
    setPairSelectionIds(prev => {
      if (prev.includes(id)) {
        return prev.filter(item => item !== id)
      }
      return [...prev, id]
    })
  }, [inRangeIds, selectedId])

  return {
    robots,
    selectedId,
    activeState,
    reset,
    selectRobot,
    encryptionPartners,
    inRangeIds,
    encryptionMode,
    pairSelectionIds,
    enterPairMode,
    enterGroupMode,
    exitPairMode,
    togglePairCandidate
  }
}

const RobotControlPage: React.FC = () => {
  const {
    robots,
    selectedId,
    activeState,
    reset,
    selectRobot,
    encryptionPartners,
    inRangeIds,
    encryptionMode,
    pairSelectionIds,
    enterPairMode,
    enterGroupMode,
    exitPairMode,
    togglePairCandidate
  } = useRobotControl()
  const horizonClipId = useMemo(() => `horizon-clip-${Math.random().toString(36).slice(2, 9)}`, [])
  const selectedRobotName = useMemo(() => {
    return robots.find(robot => robot.id === selectedId)?.name ?? '当前无人机'
  }, [robots, selectedId])

  const pairMode = encryptionMode === 'pair'
  const groupMode = encryptionMode === 'group'

  const selectedPartners = useMemo(() => (
    pairSelectionIds
      .map(id => robots.find(robot => robot.id === id))
      .filter((robot): robot is SceneRobot => Boolean(robot))
  ), [pairSelectionIds, robots])

  const selectedPartnerNames = useMemo(() => selectedPartners.map(robot => robot.name), [selectedPartners])
  const selectionCount = selectedPartners.length

  const pairButtonDisabled = groupMode ? true : (!pairMode && selectionCount !== 1)
  const groupButtonDisabled = pairMode ? true : (!groupMode && selectionCount < 2)

  const selectionSummary = useMemo(() => {
    const names = [selectedRobotName, ...selectedPartnerNames]
    return names.filter(Boolean).join('、')
  }, [selectedRobotName, selectedPartnerNames])

  const handlePairEncryption = useCallback(() => {
    if (pairMode) {
      message.info('成对加密已取消')
      exitPairMode({ clearSelection: false })
      return
    }

    if (selectionCount !== 1) {
      message.warning('请先选择 1 台协同无人机')
      return
    }

    const partner = selectedPartners[0]
    if (!partner) return

    enterPairMode()
    message.success(`已建立成对加密：${selectedRobotName} ⇆ ${partner.name}`)
  }, [pairMode, selectionCount, selectedPartners, selectedRobotName, enterPairMode, exitPairMode])

  const handleGroupEncryption = useCallback(() => {
    if (groupButtonDisabled) return

    if (groupMode) {
      message.info('组加密已取消')
      exitPairMode({ clearSelection: false })
      return
    }

    if (selectionCount < 2) {
      message.warning('请至少选择 2 台协同无人机')
      return
    }

    const names = [selectedRobotName, ...selectedPartnerNames]
    message.success(`已建立组加密：${names.join('、')}`)
    enterGroupMode()
  }, [groupButtonDisabled, groupMode, selectionCount, selectedPartnerNames, selectedRobotName, enterGroupMode, exitPairMode])

  const handlePartnerPick = useCallback((robotId: string) => {
    if (!inRangeIds.includes(robotId)) return
    togglePairCandidate(robotId)
  }, [inRangeIds, togglePairCandidate])

  const stats = useMemo(() => ([
    { label: '当前深度', value: `${activeState.sensors.depth.toFixed(2)} m` },
    { label: '水温', value: `${activeState.sensors.temperature.toFixed(1)} ℃` },
    { label: '航向角', value: `${activeState.sensors.heading.toFixed(1)} °` },
    { label: '俯仰角', value: `${activeState.sensors.pitch.toFixed(1)} °` },
    { label: '横滚角', value: `${activeState.sensors.roll.toFixed(1)} °` },
    { label: '剩余电量', value: `${activeState.sensors.battery.toFixed(1)} %` }
  ]), [activeState.sensors])

  const controlInstructions = useMemo(() => ([
    { keys: ['W'], desc: '前进' },
    { keys: ['S'], desc: '后退' },
    { keys: ['A'], desc: '左移' },
    { keys: ['D'], desc: '右移' },
    { keys: ['Q', '空格'], desc: '上浮' },
    { keys: ['E', 'Shift'], desc: '下潜' },
    { keys: ['J'], desc: '向左调整航向' },
    { keys: ['L'], desc: '向右调整航向' }
  ]), [])

  const formattedTime = useMemo(() => {
    return new Date(activeState.timestamp).toLocaleTimeString('zh-CN', { hour12: false })
  }, [activeState.timestamp])

  const renderCompass = (heading: number) => {
    const ticks = Array.from({ length: 12 }, (_, idx) => idx * 30)
    return (
      <svg viewBox="0 0 140 140" className={styles.instrumentSvg}>
        <circle cx="70" cy="70" r="55" className={styles.compassRing} />
        {ticks.map(angle => {
          const rad = MathUtils.degToRad(angle)
          const x1 = 70 + Math.sin(rad) * 46
          const y1 = 70 - Math.cos(rad) * 46
          const x2 = 70 + Math.sin(rad) * 52
          const y2 = 70 - Math.cos(rad) * 52
          return <line key={angle} x1={x1} y1={y1} x2={x2} y2={y2} className={styles.compassTick} />
        })}
        <g transform="translate(70,70)">
          <polygon
            points="0,-42 6,0 0,8 -6,0"
            transform={`rotate(${heading})`}
            className={styles.compassNeedleNorth}
          />
          <polygon
            points="0,42 -6,0 0,-8 6,0"
            transform={`rotate(${heading})`}
            className={styles.compassNeedleSouth}
          />
        </g>
        <text x="70" y="18" className={styles.compassLabel}>N</text>
        <text x="122" y="76" className={styles.compassLabel}>E</text>
        <text x="70" y="128" className={styles.compassLabel}>S</text>
        <text x="18" y="76" className={styles.compassLabel}>W</text>
        <text x="70" y="84" className={styles.compassLabel}>{heading.toFixed(0)}°</text>
      </svg>
    )
  }

  const renderHorizon = (pitch: number, roll: number) => {
    const radius = 55
    const clampedPitch = MathUtils.clamp(pitch, -35, 35)
    const clampedRoll = MathUtils.clamp(roll, -60, 60)
    const rollDeg = clampedRoll
    const pitchScale = 1.18
    const horizonOffset = MathUtils.clamp(clampedPitch * pitchScale, -radius + 6, radius - 6)
    const pitchTicks = [-20, -10, 0, 10, 20]
    const bankTicks = [-20, -10, 10, 20]

    const renderPitchTick = (angle: number) => {
      const isMajor = angle % 20 === 0
      const lineLength = isMajor ? radius * 0.18 : radius * 0.08
      const y = MathUtils.clamp((clampedPitch - angle) * pitchScale, -radius + 8, radius - 8)
      return (
        <g key={`pitch-${angle}`}>
          <line
            x1={-lineLength}
            y1={y}
            x2={lineLength}
            y2={y}
            className={isMajor ? `${styles.horizonPitchTick} ${styles.horizonPitchTickMajor}` : styles.horizonPitchTick}
          />
          {isMajor && angle !== 0 && (
            <>
              <text x={lineLength + 10} y={y + 4} className={styles.horizonPitchLabel}>{Math.abs(angle)}</text>
              <text x={-lineLength - 10} y={y + 4} className={styles.horizonPitchLabel} textAnchor="end">{Math.abs(angle)}</text>
            </>
          )}
        </g>
      )
    }

    return (
      <svg viewBox="0 0 140 140" className={styles.instrumentSvg}>
        <defs>
          <clipPath id={horizonClipId}>
            <circle cx="70" cy="70" r={radius} />
          </clipPath>
        </defs>
        <circle cx="70" cy="70" r={radius} className={styles.horizonOuterRing} />
        <g clipPath={`url(#${horizonClipId})`}>
          <g transform={`translate(70,70) rotate(${rollDeg})`}>
            <rect x={-radius * 2} y={-radius * 2} width={radius * 4} height={radius * 2 + horizonOffset} className={styles.horizonSky} />
            <rect x={-radius * 2} y={horizonOffset} width={radius * 4} height={radius * 2 - horizonOffset} className={styles.horizonGround} />
            <rect x={-radius} y={horizonOffset - 2} width={radius * 2} height={4} className={styles.horizonSeparator} />
            {pitchTicks.map(renderPitchTick)}
          </g>
        </g>
        <g transform="translate(70,70)">
          <circle r={radius} className={styles.horizonInnerBorder} />
          <g className={styles.horizonBankIndicator}>
            <path d={`M0,-${radius - 10} L7,-${radius - 2} L-7,-${radius - 2} Z`} className={styles.horizonBankPointer} />
            {bankTicks.map(angle => {
              const isMajor = angle % 30 === 0
              const len = isMajor ? 12 : 7
              const rad = MathUtils.degToRad(angle)
              const x1 = Math.sin(rad) * (radius - 4)
              const y1 = -Math.cos(rad) * (radius - 4)
              const x2 = Math.sin(rad) * (radius - 4 - len)
              const y2 = -Math.cos(rad) * (radius - 4 - len)
              return <line key={`bank-${angle}`} x1={x1} y1={y1} x2={x2} y2={y2} className={styles.horizonBankTick} />
            })}
          </g>
          <g>
            <line x1={-30} y1={0} x2={30} y2={0} className={styles.horizonAircraftWing} />
            <circle r={5} className={styles.horizonAircraftHub} />
          </g>
        </g>
      </svg>
    )
  }

  const thrusterData = useMemo(() => ([
    {
      label: '前向推进',
      value: activeState.thrusters.forward,
      max: MAX_FORWARD_THRUST,
      speed: activeState.velocity.forward,
      unit: 'm/s'
    },
    {
      label: '横向推进',
      value: activeState.thrusters.strafe,
      max: MAX_STRAFE_THRUST,
      speed: activeState.velocity.strafe,
      unit: 'm/s'
    },
    {
      label: '垂向推进',
      value: activeState.thrusters.vertical,
      max: MAX_VERTICAL_THRUST,
      speed: activeState.velocity.vertical,
      unit: 'm/s'
    },
    {
      label: '航向调整',
      value: activeState.thrusters.yaw,
      max: MAX_YAW_THRUST,
      speed: (activeState.thrusters.yaw / MAX_YAW_THRUST) * MAX_YAW_RATE,
      unit: '°/s'
    }
  ]), [activeState.thrusters, activeState.velocity])

  const formatSpeed = (value: number, unit: string) => {
    const decimals = unit === '°/s' ? 1 : 2
    const sign = value > 0 ? '' : value < 0 ? '-' : ''
    return `${sign}${Math.abs(value).toFixed(decimals)} ${unit}`
  }

  const renderThrusterBar = (value: number, maxMagnitude: number) => {
    const clamped = MathUtils.clamp(value, -maxMagnitude, maxMagnitude)
    const positiveWidth = clamped > 0 ? `${(Math.abs(clamped) / maxMagnitude * 50).toFixed(2)}%` : '0%'
    const negativeWidth = clamped < 0 ? `${(Math.abs(clamped) / maxMagnitude * 50).toFixed(2)}%` : '0%'

    return (
      <div className={styles.thrusterTrack}>
        <div
          className={`${styles.thrusterFill} ${styles.thrusterNegative}`}
          style={{ width: negativeWidth }}
        />
        <div
          className={`${styles.thrusterFill} ${styles.thrusterPositive}`}
          style={{ width: positiveWidth }}
        />
      </div>
    )
  }

  const batteryValue = activeState.sensors.battery
  const batteryFillPercent = MathUtils.clamp(batteryValue, 0, 100)
  const batteryDisplayText = `${batteryValue.toFixed(1)} %`
  const batteryLevelClass =
    batteryFillPercent <= 20
      ? styles.batteryLevelLow
      : batteryFillPercent <= 50
        ? styles.batteryLevelMedium
        : styles.batteryLevelHigh

  return (
    <div className={styles.container}>
      <div className={styles.scenePanel}>
        <div className={styles.sceneHeader}>
          <div>
            <Title level={3} className={styles.sceneTitle}>水下机器人网络</Title>
            <div className={styles.sceneStatus}>
              <Tag color="blue">
                <ThunderboltOutlined />
              </Tag>
              <span><AimOutlined style={{ marginRight: 4 }} /> 最近刷新：{formattedTime}</span>
            </div>
          </div>
          <Space>
            <Button type="default" icon={<ReloadOutlined />} onClick={reset}>
              重置姿态
            </Button>
          </Space>
        </div>
        <div className={styles.sceneTopbar}>
          <div className={styles.robotToggleGroup}>
            {robots.map(robot => (
              <button
                key={robot.id}
                type="button"
                onClick={() => selectRobot(robot.id)}
                className={`${styles.robotToggle}${selectedId === robot.id ? ` ${styles.robotToggleActive}` : ''}`}
                style={selectedId === robot.id ? { borderColor: robot.accent, color: robot.accent } : undefined}
              >
                {robot.name}
              </button>
            ))}
          </div>
        </div>
        <div className={styles.sceneCanvasWrapper}>
          <div className={styles.canvasWrapper}>
            <RobotScene
              robots={robots}
              selectedId={selectedId}
              onSelect={selectRobot}
              encryptionRadius={ENCRYPTION_RADIUS}
              inRangeIds={inRangeIds}
              pairSelectionIds={pairSelectionIds}
              encryptionMode={encryptionMode}
              onPairToggle={togglePairCandidate}
            />
            <div className={styles.batteryOverlay}>
              <div className={styles.batteryCard}>
                <div className={styles.batteryIcon}>
                  <div className={styles.batteryShell}>
                    <div
                      className={`${styles.batteryLevel} ${batteryLevelClass}`}
                      style={{ width: `${batteryFillPercent}%` }}
                    />
                  </div>
                  <div className={styles.batteryTip} />
                </div>
                <span className={styles.batteryPercent}>{batteryDisplayText}</span>
              </div>
            </div>
            <div className={styles.thrusterOverlay}>
              <Card
                bordered={false}
                className={styles.thrusterCard}
                bodyStyle={{ paddingBottom: 12 }}
              >
                <div className={styles.thrusterList}>
                  {thrusterData.map(item => (
                    <div key={item.label} className={styles.thrusterRow}>
                      <span className={styles.thrusterLabel}>{item.label}</span>
                      <div className={styles.thrusterMeter}>
                        {renderThrusterBar(item.value, item.max)}
                        <span className={styles.thrusterValue}>
                          {formatSpeed(item.speed, item.unit)}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </Card>
            </div>
          </div>
        </div>
        <div className={styles.sceneToolbar}>
          <div className={styles.controlsListInline}>
            {controlInstructions.map(item => (
              <div key={item.desc} className={styles.controlItemInline}>
                {item.keys.map(key => (
                  <span key={key} className={styles.keyTag}>{key}</span>
                ))}
                <Text>{item.desc}</Text>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className={styles.infoPanel}>
        <Card
          title={<Space><CompassOutlined /> 环境与姿态</Space>}
          bordered={false}
          className={styles.infoCard}
          bodyStyle={{ paddingBottom: 0 }}
        >
          <div className={styles.instrumentSection}>
            <div className={styles.instrument}>
              {renderCompass(activeState.sensors.heading)}
            </div>
            <div className={styles.instrument}>
              {renderHorizon(activeState.orientation.pitch, activeState.orientation.roll)}
            </div>
          </div>
          <div className={styles.statsGrid}>
            {stats.map(item => (
              <div key={item.label} className={styles.statItem}>
                <div className={styles.statLabel}>{item.label}</div>
                <div className={styles.statValue}>{item.value}</div>
              </div>
            ))}
          </div>
          <Divider style={{ borderColor: 'rgba(145,213,255,0.3)', margin: '16px 0 0' }} />
        </Card>

        <Card
          title={<Space><ThunderboltOutlined /> 加密协作</Space>}
          bordered={false}
          className={styles.infoCard}
        >
          <div className={styles.encryptionActions}>
            <Button
              type="primary"
              disabled={pairButtonDisabled}
              className={pairMode ? styles.encryptionButtonActive : undefined}
              onClick={handlePairEncryption}
            >
              {pairMode ? '取消成对加密' : '成对加密'}
            </Button>
            <Button
              type="primary"
              disabled={groupButtonDisabled}
              className={groupMode ? styles.encryptionButtonActive : undefined}
              onClick={handleGroupEncryption}
            >
              {groupMode ? '取消成组加密' : '成组加密'}
            </Button>
          </div>

          <div className={styles.encryptionSelectionLine}>
            当前已选择：{selectionSummary || '当前无人机'}
          </div>
          <div className={styles.encryptionHint}>范围内的无人机（点击选择/取消）</div>

          {encryptionPartners.length === 0 ? (
            <div className={styles.encryptionEmpty}>范围内暂无其他无人机，等待接入…</div>
          ) : (
            <div className={styles.encryptionList}>
              {encryptionPartners.map(({ robot, distance }) => (
                <div
                  key={robot.id}
                  className={([
                    styles.encryptionItem,
                    pairSelectionIds.includes(robot.id) ? styles.encryptionItemActive : '',
                    inRangeIds.includes(robot.id) ? styles.encryptionItemSelectable : ''
                  ].filter(Boolean) as string[]).join(' ')}
                  onClick={() => handlePartnerPick(robot.id)}
                >
                  <span className={styles.encryptionName}>{robot.name}</span>
                  <span className={styles.encryptionMeta}>
                    距离 {distance.toFixed(1)} m
                  </span>
                </div>
              ))}
            </div>
          )}
        </Card>

      </div>
    </div>
  )
}

export default RobotControlPage
