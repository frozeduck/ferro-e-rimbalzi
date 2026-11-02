import React, { useState, useRef, useEffect, useCallback } from 'react';
import { GameEngine } from './game/GameEngine';
import { GameCanvas } from './components/GameCanvas';
import { GameHeader } from './components/GameHeader';
import { GameControls } from './components/GameControls';
import { HubView } from './components/HubView';
import { UpgradeModal } from './components/UpgradeModal';
import { ResultModal } from './components/ResultModal';
import { TutorialModal } from './components/TutorialModal';
import { PauseModal } from './components/PauseModal';
import { AudioHelper } from './audio/AudioSynthesizer';
import { BiomeType, Building } from './types/game';

export const App: React.FC = () => {
  const gameRef = useRef<GameEngine>(new GameEngine());
  const game = gameRef.current;

  const [screen, setScreen] = useState<'hub' | 'game'>(game.screen);
  const [phase, setPhase] = useState(game.phase);
  const [paused, setPaused] = useState(false);
  const [showTutorial, setShowTutorial] = useState(false);
  const [soundEnabled, setSoundEnabled] = useState(AudioHelper.enabled);
  const [placingType, setPlacingType] = useState<Building['type'] | null>(null);
  const [, setTick] = useState(0);

  const forceUpdate = useCallback(() => {
    setScreen(game.screen);
    setPhase(game.phase);
    setTick((t) => t + 1);
  }, [game]);

  const handleStartExpedition = (biome: BiomeType) => {
    game.startExpedition(biome);
    setPaused(false);
    setPlacingType(null);
    forceUpdate();
  };

  const handleReturnToHub = () => {
    game.returnToHub();
    setPaused(false);
    forceUpdate();
  };

  const handleRestart = () => {
    game.startExpedition(game.selectedBiome);
    setPaused(false);
    setPlacingType(null);
    forceUpdate();
  };

  const handleSoundToggle = () => {
    const nextState = AudioHelper.toggleSound();
    setSoundEnabled(nextState);
  };

  const handleSelectUpgrade = (type: 'cargo' | 'paddle' | 'damage') => {
    game.chooseUpgrade(type);
    forceUpdate();
  };

  // Keyboard controls (Space to launch / pause, Left / Right arrow keys for paddle)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === ' ' || e.code === 'Space') {
        if (game.screen === 'game') {
          if (game.phase === 'wave' && game.ball.held) {
            if (game.launch()) {
              AudioHelper.playAnvil();
              forceUpdate();
            }
          }
        }
      } else if (e.key === 'p' || e.key === 'P' || e.key === 'Escape') {
        if (game.screen === 'game') {
          setPaused((p) => !p);
        }
      } else if (e.key === 'ArrowLeft' || e.key === 'a' || e.key === 'A') {
        if (game.screen === 'game' && game.phase === 'wave') {
          game.paddle.x = Math.max(
            game.paddleWidth / 2 + 14,
            game.paddle.x - 24
          );
          if (game.ball.held) game.ball.x = game.paddle.x;
          forceUpdate();
        }
      } else if (e.key === 'ArrowRight' || e.key === 'd' || e.key === 'D') {
        if (game.screen === 'game' && game.phase === 'wave') {
          game.paddle.x = Math.min(
            GameEngine.W - game.paddleWidth / 2 - 14,
            game.paddle.x + 24
          );
          if (game.ball.held) game.ball.x = game.paddle.x;
          forceUpdate();
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [game, forceUpdate]);

  return (
    <div className="w-full h-full flex justify-center items-center bg-[#0d0704] overflow-hidden select-none">
      {screen === 'hub' ? (
        <HubView
          game={game}
          onStartExpedition={handleStartExpedition}
          onOpenTutorial={() => setShowTutorial(true)}
          onSoundToggle={handleSoundToggle}
          soundEnabled={soundEnabled}
        />
      ) : (
        <div className="relative w-full h-full max-w-[540px] max-h-[960px] aspect-[9/16] bg-black overflow-hidden flex flex-col justify-between">
          {/* Header */}
          <GameHeader
            game={game}
            onHelp={() => setShowTutorial(true)}
            onPause={() => setPaused(true)}
            onSoundToggle={handleSoundToggle}
            soundEnabled={soundEnabled}
          />

          {/* Canvas */}
          <GameCanvas
            game={game}
            paused={paused || showTutorial}
            onOpenTutorial={() => setShowTutorial(true)}
            onTogglePause={() => setPaused((p) => !p)}
            placingType={placingType}
            setPlacingType={setPlacingType}
            onStateChange={forceUpdate}
          />

          {/* Bottom Action Bar */}
          <GameControls
            game={game}
            placingType={placingType}
            setPlacingType={setPlacingType}
            onStateChange={forceUpdate}
          />

          {/* Modals & Overlays */}
          {phase === 'upgrade' && (
            <UpgradeModal game={game} onSelectUpgrade={handleSelectUpgrade} />
          )}

          {(phase === 'won' || phase === 'short' || phase === 'lost') && (
            <ResultModal
              game={game}
              onReturnToHub={handleReturnToHub}
              onRestart={handleRestart}
            />
          )}

          {paused && (
            <PauseModal
              onResume={() => setPaused(false)}
              onRestart={handleRestart}
              onReturnHub={handleReturnToHub}
            />
          )}

          {showTutorial && (
            <TutorialModal onClose={() => setShowTutorial(false)} />
          )}
        </div>
      )}

      {showTutorial && screen === 'hub' && (
        <TutorialModal onClose={() => setShowTutorial(false)} />
      )}
    </div>
  );
};

export default App;
