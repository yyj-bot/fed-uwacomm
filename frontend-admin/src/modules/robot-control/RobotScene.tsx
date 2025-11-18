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
      <group castShadow receiveShadow>
        <mesh position={[0, 0.55, 0]}>
          <boxGeometry args={[2.6, 0.4, 1.8]} />
          <meshStandardMaterial color={robot.accent} roughness={0.4} metalness={0.1} />
        </mesh>
        <mesh position={[0, 0.1, 0]}>
          <boxGeometry args={[2.6, 0.25, 1.8]} />
          <meshStandardMaterial color={robot.trim} roughness={0.55} metalness={0.35} />
        </mesh>
        <mesh position={[0, -0.65, 0]}>
          <boxGeometry args={[2.6, 0.22, 1.8]} />
          <meshStandardMaterial color="#3b444e" roughness={0.6} metalness={0.2} />
        </mesh>
        <mesh position={[0, -0.2, 0]}>
          <boxGeometry args={[2.6, 1.2, 1.8]} />
          <meshStandardMaterial color={robot.body} roughness={0.55} metalness={0.25} />
        </mesh>
        <mesh position={[0, -0.2, 0]}>
          <boxGeometry args={[2.3, 1.05, 1.5]} />
          <meshStandardMaterial color="#10161c" roughness={0.8} metalness={0.15} />
        </mesh>
      </group>

      <group>
        <mesh position={[-1.15, 0.05, 0]}>
          <cylinderGeometry args={[0.28, 0.28, 1.45, 24]} />
          <meshStandardMaterial color="#7bd9d3" roughness={0.4} metalness={0.3} />
        </mesh>
        <mesh position={[1.15, 0.05, 0]}>
          <cylinderGeometry args={[0.28, 0.28, 1.45, 24]} />
          <meshStandardMaterial color="#7bd9d3" roughness={0.4} metalness={0.3} />
        </mesh>

        <mesh position={[-1.35, 0.05, 0]}>
          <cylinderGeometry args={[0.2, 0.2, 0.35, 16]} />
          <meshStandardMaterial color="#cfd8df" roughness={0.4} metalness={0.35} />
        </mesh>
        <mesh position={[1.35, 0.05, 0]}>
          <cylinderGeometry args={[0.2, 0.2, 0.35, 16]} />
          <meshStandardMaterial color="#cfd8df" roughness={0.4} metalness={0.35} />
        </mesh>

        <mesh position={[0, -0.65, 0]}>
          <boxGeometry args={[1.5, 0.15, 1.4]} />
          <meshStandardMaterial color="#2b3137" roughness={0.7} metalness={0.1} />
        </mesh>
        <mesh position={[0, -0.55, 0]}>
          <boxGeometry args={[1.6, 0.02, 1.5]} />
          <meshStandardMaterial color="#06090c" roughness={0.85} metalness={0.05} />
        </mesh>

        <mesh position={[0, -0.2, 0.9]} rotation={[Math.PI / 2, 0, 0]}>
          <torusGeometry args={[0.4, 0.04, 12, 40]} />
          <meshStandardMaterial color="#6d7378" roughness={0.5} metalness={0.3} />
        </mesh>
        <mesh position={[0, -0.2, -0.9]} rotation={[Math.PI / 2, 0, 0]}>
          <torusGeometry args={[0.4, 0.04, 12, 40]} />
          <meshStandardMaterial color="#6d7378" roughness={0.5} metalness={0.3} />
        </mesh>

        <mesh position={[0, -0.12, 0]}>
          <boxGeometry args={[2.4, 0.12, 0.12]} />
          <meshStandardMaterial color="#868d96" roughness={0.45} metalness={0.3} />
        </mesh>
        <mesh position={[0, -0.45, 0]}>
          <boxGeometry args={[2.4, 0.12, 0.12]} />
          <meshStandardMaterial color="#868d96" roughness={0.45} metalness={0.3} />
        </mesh>

        <mesh position={[-0.95, -0.25, 0.7]}>
          <sphereGeometry args={[0.18, 20, 20]} />
          <meshStandardMaterial color="#7bd9d3" roughness={0.35} metalness={0.25} />
        </mesh>
        <mesh position={[0.95, -0.25, 0.7]}>
          <sphereGeometry args={[0.18, 20, 20]} />
          <meshStandardMaterial color="#7bd9d3" roughness={0.35} metalness={0.25} />
        </mesh>
        <mesh position={[-0.95, -0.25, -0.7]}>
          <sphereGeometry args={[0.18, 20, 20]} />
          <meshStandardMaterial color="#7bd9d3" roughness={0.35} metalness={0.25} />
        </mesh>
        <mesh position={[0.95, -0.25, -0.7]}>
          <sphereGeometry args={[0.18, 20, 20]} />
          <meshStandardMaterial color="#7bd9d3" roughness={0.35} metalness={0.25} />
        </mesh>
      </group>

      <group>
        <mesh position={[0, 0.75, -0.3]} rotation={[0, Math.PI / 2, 0]}>
          <cylinderGeometry args={[0.15, 0.15, 0.55, 16]} />
          <meshStandardMaterial color="#cfd8df" roughness={0.35} metalness={0.4} />
        </mesh>
        <mesh position={[0, 0.75, 0.35]} rotation={[0, Math.PI / 2, 0]}>
          <cylinderGeometry args={[0.15, 0.15, 0.55, 16]} />
          <meshStandardMaterial color="#cfd8df" roughness={0.35} metalness={0.4} />
        </mesh>
      </group>

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
