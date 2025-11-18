import React, { useEffect, useMemo, useRef } from 'react'
import { Canvas, useFrame } from '@react-three/fiber'
import { OrbitControls, Html } from '@react-three/drei'
import * as THREE from 'three'

import type { SceneRobot } from './types'

interface RobotSceneProps {
  robots: SceneRobot[]
  selectedId: string
  onSelect: (id: string) => void
  encryptionRadius: number
  inRangeIds: string[]
  pairSelectionIds: string[]
  encryptionMode: 'idle' | 'pair' | 'group'
  onPairToggle: (id: string) => void
}

interface RobotMeshProps {
  robot: SceneRobot
  isSelected: boolean
  isInRange: boolean
  isPairSelected: boolean
  encryptionMode: 'idle' | 'pair' | 'group'
  onSelect: () => void
  onPairToggle: () => void
}

const createRoundedRectShape = (width: number, height: number, radius: number, offsetX = 0, offsetY = 0) => {
  const shape = new THREE.Shape()
  const hw = width / 2
  const hh = height / 2
  const r = Math.min(radius, hw, hh)

  shape.moveTo(-hw + r + offsetX, -hh + offsetY)
  shape.lineTo(hw - r + offsetX, -hh + offsetY)
  shape.quadraticCurveTo(hw + offsetX, -hh + offsetY, hw + offsetX, -hh + r + offsetY)
  shape.lineTo(hw + offsetX, hh - r + offsetY)
  shape.quadraticCurveTo(hw + offsetX, hh + offsetY, hw - r + offsetX, hh + offsetY)
  shape.lineTo(-hw + r + offsetX, hh + offsetY)
  shape.quadraticCurveTo(-hw + offsetX, hh + offsetY, -hw + offsetX, hh - r + offsetY)
  shape.lineTo(-hw + offsetX, -hh + r + offsetY)
  shape.quadraticCurveTo(-hw + offsetX, -hh + offsetY, -hw + r + offsetX, -hh + offsetY)

  return shape
}

interface ThrusterProps {
  position: [number, number, number]
  rotation?: [number, number, number]
  accent: string
}

const Thruster: React.FC<ThrusterProps> = ({ position, rotation = [0, 0, 0], accent }) => {
  const housingGeometry = useMemo(() => new THREE.CylinderGeometry(0.36, 0.36, 0.62, 36, 1, false), [])
  const coreGeometry = useMemo(() => new THREE.CylinderGeometry(0.18, 0.18, 0.32, 24), [])
  const bladeGeometry = useMemo(() => new THREE.BoxGeometry(0.08, 0.48, 0.1), [])
  const guardRingGeometry = useMemo(() => new THREE.TorusGeometry(0.36, 0.04, 14, 48), [])

  const bladeAngles = useMemo(() => [0, Math.PI / 2.6, -Math.PI / 2.6, Math.PI], [])

  return (
    <group position={position} rotation={rotation}>
      <mesh rotation={[Math.PI / 2, 0, 0]} geometry={housingGeometry} castShadow receiveShadow>
        <meshStandardMaterial color="#7cdfd6" roughness={0.38} metalness={0.32} />
      </mesh>
      <mesh rotation={[Math.PI / 2, 0, 0]} geometry={coreGeometry} castShadow receiveShadow>
        <meshStandardMaterial color="#96e2db" roughness={0.35} metalness={0.42} />
      </mesh>
      <group rotation={[0, 0, Math.PI / 6]}>
        {bladeAngles.map(angle => (
          <mesh key={`blade-${angle}`} rotation={[0, 0, angle]} geometry={bladeGeometry} castShadow receiveShadow>
            <meshStandardMaterial color={accent} roughness={0.28} metalness={0.4} />
          </mesh>
        ))}
      </group>
      <mesh rotation={[Math.PI / 2, 0, 0]} geometry={guardRingGeometry}>
        <meshStandardMaterial color="#6abbbc" roughness={0.35} metalness={0.45} />
      </mesh>
      <mesh position={[0, -0.45, 0]} rotation={[Math.PI / 2, 0, 0]}>
        <cylinderGeometry args={[0.08, 0.08, 0.9, 16]} />
        <meshStandardMaterial color="#c9d5dd" roughness={0.5} metalness={0.38} />
      </mesh>
      <mesh position={[0, -0.45, 0]} rotation={[Math.PI / 2, Math.PI / 2, 0]}>
        <cylinderGeometry args={[0.04, 0.04, 0.9, 16]} />
        <meshStandardMaterial color="#9aa3ad" roughness={0.55} metalness={0.32} />
      </mesh>
    </group>
  )
}

const VerticalThruster: React.FC<{ accent: string }> = ({ accent }) => {
  const shroudGeometry = useMemo(() => new THREE.CylinderGeometry(0.24, 0.24, 0.44, 32), [])
  const bladeGeometry = useMemo(() => new THREE.BoxGeometry(0.06, 0.32, 0.06), [])
  const bladeAngles = useMemo(() => [0, Math.PI / 2, Math.PI, (3 * Math.PI) / 2], [])

  return (
    <group position={[0, 0.55, 0]}>
      <mesh geometry={shroudGeometry} castShadow receiveShadow>
        <meshStandardMaterial color="#83d8d0" roughness={0.38} metalness={0.35} />
      </mesh>
      <mesh rotation={[Math.PI / 2, 0, 0]}>
        <cylinderGeometry args={[0.1, 0.1, 0.22, 20]} />
        <meshStandardMaterial color="#a7ece7" roughness={0.32} metalness={0.42} />
      </mesh>
      {bladeAngles.map(angle => (
        <mesh key={`v-blade-${angle}`} rotation={[0, angle, 0]} geometry={bladeGeometry}>
          <meshStandardMaterial color={accent} roughness={0.28} metalness={0.4} />
        </mesh>
      ))}
      <mesh position={[0, -0.4, 0]}>
        <cylinderGeometry args={[0.05, 0.05, 0.38, 12]} />
        <meshStandardMaterial color="#cdd6df" roughness={0.52} metalness={0.32} />
      </mesh>
    </group>
  )
}

const FrameShell: React.FC<{ body: string; trim: string }> = ({ body, trim }) => {
  const frameColor = '#8f97a4'
  const braceColor = '#b7c0ca'
  const footingColor = '#5f6670'

  const topBeamY = 0.74
  const bottomBeamY = -0.84
  const midBeamY = -0.08

  return (
    <group>
      {/* 四角立柱 */}
      {[-1.28, 1.28].map(x => (
        [0.92, -0.92].map(z => (
          <group key={`pillar-${x}-${z}`}>
            <mesh position={[x, -0.05, z]}>
              <cylinderGeometry args={[0.085, 0.085, 1.64, 22]} />
              <meshStandardMaterial color={frameColor} roughness={0.54} metalness={0.3} />
            </mesh>
            <mesh position={[x, topBeamY + 0.06, z]}>
              <cylinderGeometry args={[0.12, 0.12, 0.12, 20]} />
              <meshStandardMaterial color={braceColor} roughness={0.48} metalness={0.28} />
            </mesh>
            <mesh position={[x, bottomBeamY - 0.08, z]}>
              <cylinderGeometry args={[0.12, 0.14, 0.14, 20]} />
              <meshStandardMaterial color={footingColor} roughness={0.6} metalness={0.22} />
            </mesh>
          </group>
        ))
      ))}

      {/* 顶部水平梁 */}
      {[-0.92, 0.92].map(z => (
        <mesh key={`top-long-${z}`} position={[0, topBeamY, z]}>
          <boxGeometry args={[2.48, 0.14, 0.12]} />
          <meshStandardMaterial color={frameColor} roughness={0.5} metalness={0.3} />
        </mesh>
      ))}
      {[-1.24, 1.24].map(x => (
        <mesh key={`top-short-${x}`} position={[x, topBeamY, 0]}>
          <boxGeometry args={[0.12, 0.14, 1.84]} />
          <meshStandardMaterial color={frameColor} roughness={0.5} metalness={0.3} />
        </mesh>
      ))}

      {/* 中部轻型支撑 */}
      {[-0.92, 0.92].map(z => (
        <mesh key={`mid-long-${z}`} position={[0, midBeamY, z]}>
          <boxGeometry args={[2.48, 0.08, 0.08]} />
          <meshStandardMaterial color={braceColor} roughness={0.55} metalness={0.25} />
        </mesh>
      ))}
      {[-1.24, 1.24].map(x => (
        <mesh key={`mid-short-${x}`} position={[x, midBeamY, 0]}>
          <boxGeometry args={[0.08, 0.08, 1.84]} />
          <meshStandardMaterial color={braceColor} roughness={0.55} metalness={0.25} />
        </mesh>
      ))}

      {/* 底部加强梁 */}
      {[-0.92, 0.92].map(z => (
        <mesh key={`bottom-long-${z}`} position={[0, bottomBeamY, z]}>
          <boxGeometry args={[2.48, 0.16, 0.14]} />
          <meshStandardMaterial color={frameColor} roughness={0.56} metalness={0.28} />
        </mesh>
      ))}
      {[-1.24, 1.24].map(x => (
        <mesh key={`bottom-short-${x}`} position={[x, bottomBeamY, 0]}>
          <boxGeometry args={[0.14, 0.16, 1.84]} />
          <meshStandardMaterial color={frameColor} roughness={0.56} metalness={0.28} />
        </mesh>
      ))}

      {/* 底部滑轨保留但调成灰色 */}
      <mesh position={[-0.65, -0.98, 0]}>
        <boxGeometry args={[0.32, 0.12, 1.95]} />
        <meshStandardMaterial color={footingColor} roughness={0.62} metalness={0.22} />
      </mesh>
      <mesh position={[0.65, -0.98, 0]}>
        <boxGeometry args={[0.32, 0.12, 1.95]} />
        <meshStandardMaterial color={footingColor} roughness={0.62} metalness={0.22} />
      </mesh>
    </group>
  )
}

const TopCover: React.FC<{ accent: string }> = ({ accent }) => {
  const plateGeometry = useMemo(() => {
    const shape = createRoundedRectShape(2.85, 2.05, 0.46)
    const frontWell = createRoundedRectShape(1.2, 0.72, 0.2, 0.2, -0.2)
    const rearWell = createRoundedRectShape(0.72, 0.52, 0.18, -0.65, 0.3)
    shape.holes.push(frontWell)
    shape.holes.push(rearWell)
    const geometry = new THREE.ExtrudeGeometry(shape, { depth: 0.16, bevelEnabled: false })
    geometry.center()
    return geometry
  }, [])

  return (
    <group position={[0, 0.92, 0]}>
      <mesh geometry={plateGeometry} rotation={[-Math.PI / 2, 0, 0]} castShadow receiveShadow>
        <meshStandardMaterial color={accent} roughness={0.45} metalness={0.22} />
      </mesh>
      <mesh position={[0.95, -0.04, 0.32]} rotation={[-Math.PI / 2, 0, 0]}>
        <cylinderGeometry args={[0.18, 0.18, 0.12, 24]} />
        <meshStandardMaterial color="#f9e977" roughness={0.38} metalness={0.28} />
      </mesh>
      <mesh position={[-0.88, -0.05, -0.28]} rotation={[-Math.PI / 2, 0, 0]}>
        <cylinderGeometry args={[0.22, 0.18, 0.18, 24]} />
        <meshStandardMaterial color="#e9dc6f" roughness={0.4} metalness={0.25} />
      </mesh>
      <mesh position={[0, -0.09, 0]}>
        <boxGeometry args={[2.2, 0.08, 1.5]} />
        <meshStandardMaterial color="#dedfce" roughness={0.52} metalness={0.2} />
      </mesh>
    </group>
  )
}

const InternalRig: React.FC<{ accent: string; trim: string }> = ({ accent, trim }) => (
  <group>
    <mesh position={[0, -0.25, 0]}>
      <boxGeometry args={[2.0, 0.18, 1.4]} />
      <meshStandardMaterial color="#0b0f14" roughness={0.78} metalness={0.12} />
    </mesh>
    <mesh position={[0, -0.1, 0]}>
      <boxGeometry args={[2.15, 0.08, 1.55]} />
      <meshStandardMaterial color="#1c232c" roughness={0.72} metalness={0.18} />
    </mesh>
    <mesh position={[0, 0.12, 0]}>
      <boxGeometry args={[1.9, 0.14, 1.2]} />
      <meshStandardMaterial color="#13181f" roughness={0.75} metalness={0.15} />
    </mesh>
    <mesh position={[0, 0.36, 0]}>
      <boxGeometry args={[1.65, 0.1, 0.95]} />
      <meshStandardMaterial color="#2d333d" roughness={0.6} metalness={0.22} />
    </mesh>
    <mesh position={[0, 0.26, 0]}>
      <boxGeometry args={[1.72, 0.04, 1.05]} />
      <meshStandardMaterial color="#39414b" roughness={0.58} metalness={0.24} />
    </mesh>

    {[-0.65, 0.65].map(x => (
      <mesh key={`rig-strut-${x}`} position={[x, 0.18, 0]} rotation={[0, 0, -Math.PI / 9]}>
        <cylinderGeometry args={[0.05, 0.05, 1.65, 16]} />
        <meshStandardMaterial color={trim} roughness={0.48} metalness={0.32} />
      </mesh>
    ))}

    <mesh position={[0, -0.4, 0.82]}>
      <boxGeometry args={[1.22, 0.08, 0.12]} />
      <meshStandardMaterial color="#8aa5ba" roughness={0.52} metalness={0.28} />
    </mesh>
    <mesh position={[0, -0.4, -0.82]}>
      <boxGeometry args={[1.22, 0.08, 0.12]} />
      <meshStandardMaterial color="#8aa5ba" roughness={0.52} metalness={0.28} />
    </mesh>

    <group position={[0, -0.58, 0]}>      
      <mesh position={[0, 0, 0]}>
        <boxGeometry args={[1.42, 0.08, 0.24]} />
        <meshStandardMaterial color={accent} roughness={0.4} metalness={0.3} />
      </mesh>
      <mesh position={[0, -0.14, 0]}>
        <boxGeometry args={[1.38, 0.04, 0.28]} />
        <meshStandardMaterial color="#25303a" roughness={0.65} metalness={0.2} />
      </mesh>
    </group>

    <group position={[0, -0.1, 0.58]}>
      <mesh>
        <cylinderGeometry args={[0.08, 0.08, 0.98, 14]} />
        <meshStandardMaterial color="#98a5b5" roughness={0.55} metalness={0.3} />
      </mesh>
      <mesh position={[0, 0.18, 0]}>
        <sphereGeometry args={[0.11, 18, 18]} />
        <meshStandardMaterial color="#7fd2cb" roughness={0.38} metalness={0.32} />
      </mesh>
    </group>

    <group position={[0, -0.1, -0.58]}>
      <mesh>
        <cylinderGeometry args={[0.08, 0.08, 0.98, 14]} />
        <meshStandardMaterial color="#98a5b5" roughness={0.55} metalness={0.3} />
      </mesh>
      <mesh position={[0, 0.18, 0]}>
        <sphereGeometry args={[0.11, 18, 18]} />
        <meshStandardMaterial color="#7fd2cb" roughness={0.38} metalness={0.32} />
      </mesh>
    </group>
  </group>
)

const RobotMesh: React.FC<RobotMeshProps> = ({
  robot,
  isSelected,
  isInRange,
  isPairSelected,
  encryptionMode,
  onSelect,
  onPairToggle
}) => {
  const groupRef = useRef<THREE.Group>(null)

  useFrame(() => {
    if (!groupRef.current) return

    const { position, orientation } = robot.state
    groupRef.current.position.set(position.x, position.y, position.z)
    groupRef.current.rotation.set(
      THREE.MathUtils.degToRad(orientation.pitch),
      THREE.MathUtils.degToRad(orientation.yaw),
      THREE.MathUtils.degToRad(orientation.roll)
    )
  })

  useEffect(() => {
    return () => {
      document.body.style.cursor = 'default'
    }
  }, [])

  const horizontalThrusterPositions: [number, number, number][] = useMemo(
    () => [
      [-1.06, -0.12, 0.58],
      [-1.06, -0.12, -0.58],
      [1.06, -0.12, 0.58],
      [1.06, -0.12, -0.58]
    ],
    []
  )

  return (
    <group
      ref={groupRef}
      onClick={event => {
        event.stopPropagation()
        if (encryptionMode === 'pair') {
          if (isInRange && !isSelected) {
            onPairToggle()
          }
          return
        }
        onSelect()
        document.body.style.cursor = 'default'
      }}
      onPointerOver={event => {
        event.stopPropagation()
        if (encryptionMode === 'pair') {
          document.body.style.cursor = isInRange && !isSelected ? 'pointer' : 'not-allowed'
        } else {
          document.body.style.cursor = 'pointer'
        }
      }}
      onPointerOut={() => {
        document.body.style.cursor = 'default'
      }}
    >
      <FrameShell body={robot.body} trim={robot.trim} />
      <TopCover accent={robot.accent} />
      <InternalRig accent={robot.accent} trim={robot.trim} />

      {horizontalThrusterPositions.map((pos, index) => (
        <Thruster
          key={`thruster-${index}`}
          position={pos}
          rotation={[Math.PI / 2, 0, 0]}
          accent={robot.accent}
        />
      ))}
      <VerticalThruster accent={robot.accent} />

      {isSelected && (
        <mesh position={[0, -0.85, 0]} rotation={[-Math.PI / 2, 0, 0]}>
          <ringGeometry args={[1.5, 1.7, 48]} />
          <meshBasicMaterial color={robot.accent} transparent opacity={0.35} />
        </mesh>
      )}

      {!isSelected && isInRange && (
        <mesh position={[0, -0.85, 0]} rotation={[-Math.PI / 2, 0, 0]}>
          <ringGeometry args={[1.35, 1.55, 36]} />
          <meshBasicMaterial color={robot.accent} transparent opacity={isPairSelected ? 0.5 : 0.22} />
        </mesh>
      )}

      {!isSelected && isPairSelected && (
        <mesh position={[0, -0.85, 0]} rotation={[-Math.PI / 2, 0, 0]}>
          <ringGeometry args={[1.15, 1.25, 28]} />
          <meshBasicMaterial color={robot.accent} transparent opacity={0.6} />
        </mesh>
      )}

      {isSelected && <axesHelper args={[1.5]} />}

      <Html position={[0, 1.35, 0]} distanceFactor={16} center>
        <div className="robot-label" style={(() => {
          if (isSelected) {
            return {
              background: robot.accent,
              boxShadow: `0 10px 24px ${robot.accent}55`,
              transform: 'scale(1.05)'
            }
          }
          if (isPairSelected) {
            return {
              background: `${robot.accent}ee`,
              boxShadow: `0 8px 20px ${robot.accent}55`,
              transform: 'scale(1.04)'
            }
          }
          if (isInRange) {
            return {
              background: `${robot.accent}cc`,
              boxShadow: `0 6px 16px ${robot.accent}44`
            }
          }
          return undefined
        })()}>
          {robot.name}
        </div>
      </Html>
    </group>
  )
}

const PoolFloor: React.FC = () => (
  <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -15, 0]} receiveShadow>
    <planeGeometry args={[80, 80]} />
    <meshStandardMaterial color="#a9dfff" roughness={0.85} />
  </mesh>
)

const PoolWalls: React.FC = () => {
  const geometry = useMemo(() => {
    const box = new THREE.BoxGeometry(60, 30, 60)
    return new THREE.EdgesGeometry(box)
  }, [])

  return (
    <lineSegments geometry={geometry} position={[0, -15, 0]}>
      <lineBasicMaterial color="#4db2ff" transparent opacity={0.5} />
    </lineSegments>
  )
}

const WaterSurface: React.FC = () => (
  <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.02, 0]}>
    <planeGeometry args={[82, 82]} />
    <meshStandardMaterial
      color="#c9ecff"
      transparent
      opacity={0.45}
      roughness={0.2}
      metalness={0.05}
    />
  </mesh>
)

const RobotScene: React.FC<RobotSceneProps> = ({
  robots,
  selectedId,
  onSelect,
  encryptionRadius,
  inRangeIds,
  pairSelectionIds,
  encryptionMode,
  onPairToggle
}) => {
  const selectedRobot = useMemo(() => robots.find(robot => robot.id === selectedId), [robots, selectedId])

  return (
    <Canvas camera={{ position: [18, 18, 18], fov: 54 }} shadows>
      <color attach="background" args={["#eef8ff"]} />
      <fog attach="fog" args={["#dbefff", 60, 200]} />

      <ambientLight intensity={0.52} color="#f7fbff" />
      <directionalLight position={[18, 24, 10]} intensity={0.95} color="#ffffff" castShadow>
        <orthographicCamera
          attach="shadow-camera"
          args={[-20, 20, 20, -20, 1, 80]}
        />
      </directionalLight>
      <pointLight position={[-16, 18, -12]} intensity={0.38} color="#a6dbff" />

      {selectedRobot && (
        <group position={[selectedRobot.state.position.x, selectedRobot.state.position.y, selectedRobot.state.position.z]}>
          <mesh>
            <sphereGeometry args={[encryptionRadius, 36, 36]} />
            <meshStandardMaterial
              color={selectedRobot.accent}
              transparent
              opacity={0.08}
              roughness={0.9}
              metalness={0.05}
              depthWrite={false}
              depthTest={false}
            />
          </mesh>
          <mesh>
            <sphereGeometry args={[encryptionRadius, 18, 18]} />
            <meshBasicMaterial
              color={selectedRobot.accent}
              transparent
              opacity={0.25}
              wireframe
              depthWrite={false}
              depthTest={false}
            />
          </mesh>
        </group>
      )}

      {robots.map(robot => (
        <RobotMesh
          key={robot.id}
          robot={robot}
          isSelected={robot.id === selectedId}
          isInRange={inRangeIds.includes(robot.id)}
          isPairSelected={pairSelectionIds.includes(robot.id)}
          encryptionMode={encryptionMode}
          onSelect={() => onSelect(robot.id)}
          onPairToggle={() => onPairToggle(robot.id)}
        />
      ))}
      <PoolFloor />
      <PoolWalls />
      <WaterSurface />

      <gridHelper args={[70, 40, '#9ed6ff', '#c0e6ff']} position={[0, -15, 0]} />

      <OrbitControls
        enableDamping
        dampingFactor={0.08}
        maxPolarAngle={Math.PI * 0.62}
        minPolarAngle={0}
        target={[0, -8, 0]}
      />
    </Canvas>
  )
}

export default RobotScene
