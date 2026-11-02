import React from 'react';
import { GameEngine } from '../game/GameEngine';
import { AudioHelper } from '../audio/AudioSynthesizer';
import { Building } from '../types/game';

interface GameControlsProps {
  game: GameEngine;
  placingType: Building['type'] | null;
  setPlacingType: (type: Building['type'] | null) => void;
  onStateChange: () => void;
}

export const GameControls: React.FC<GameControlsProps> = ({
  game,
  placingType,
  setPlacingType,
  onStateChange,
}) => {
  const isCrystal = game.selectedBiome === 'crystal_cave';
  const isForest = game.selectedBiome === 'forest_wood';

  const handleForgeToggle = () => {
    game.forgeOpen = !game.forgeOpen;
    if (game.forgeOpen) AudioHelper.playForgeFire();
    else AudioHelper.playButtonClick();
    onStateChange();
  };

  const handleBurnToggle = () => {
    game.burn = !game.burn;
    AudioHelper.playButtonClick();
    onStateChange();
  };

  const handleRecall = () => {
    if (game.recallBall()) {
      AudioHelper.playWarhorn();
      onStateChange();
    }
  };

  const handleWallSelect = () => {
    const wallType = isCrystal ? 'crystal_wall' : isForest ? 'wood_wall' : 'wall';
    setPlacingType(placingType === wallType ? null : wallType);
    AudioHelper.playButtonClick();
  };

  const handleTurretSelect = () => {
    const turretType = isCrystal ? 'laser_turret' : 'turret';
    setPlacingType(placingType === turretType ? null : turretType);
    AudioHelper.playButtonClick();
  };

  const handleStartWave = () => {
    setPlacingType(null);
    if (game.startWave()) {
      AudioHelper.playWarhorn();
      onStateChange();
    }
  };

  if (game.phase === 'wave') {
    const readyRecall = game.recall <= 0 && !game.ball.held;
    return (
      <div className="absolute bottom-0 left-0 right-0 z-20 pointer-events-auto bg-gradient-to-t from-[#120A05] via-[#1E110A] to-transparent pt-3 pb-3 px-3 flex gap-2 justify-between border-t border-[#573E30]/60 select-none">
        {/* Pulsante Fucina */}
        <button
          onClick={handleForgeToggle}
          className={`flex-1 py-2 px-1 rounded-lg border flex flex-col items-center justify-center transition active:scale-95 shadow-md ${
            game.forgeOpen
              ? 'bg-[#C2410C] border-[#FFEDD5] text-amber-100 hover:bg-[#ea580c]'
              : 'bg-[#44403C] border-[#D6D3D1] text-stone-300 hover:bg-[#57534e]'
          }`}
        >
          <span className="text-xs font-black tracking-wide font-dwarven">
            {game.forgeOpen ? 'FUCINA: APERTA' : 'FUCINA: CHIUSA'}
          </span>
          <span className="text-[10px] opacity-80">
            {game.forgeOpen ? 'Fonde il Minerale' : 'Portone Riflettente'}
          </span>
        </button>

        {/* Pulsante Brucia Carico */}
        <button
          onClick={handleBurnToggle}
          className={`flex-1 py-2 px-1 rounded-lg border flex flex-col items-center justify-center transition active:scale-95 shadow-md ${
            game.burn
              ? 'bg-[#B91C1C] border-[#FEE2E2] text-red-100 hover:bg-[#dc2626]'
              : 'bg-[#047857] border-[#D1FAE5] text-emerald-100 hover:bg-[#059669]'
          }`}
        >
          <span className="text-xs font-black tracking-wide font-dwarven">
            {game.burn ? 'CARICO: ARMATO' : 'CARICO: SALVO'}
          </span>
          <span className="text-[10px] opacity-80">
            {game.burn ? 'Danno x3 (Consuma)' : 'Per il Forziere'}
          </span>
        </button>

        {/* Pulsante Richiamo */}
        <button
          onClick={handleRecall}
          disabled={!readyRecall}
          className={`flex-1 py-2 px-1 rounded-lg border flex flex-col items-center justify-center transition active:scale-95 shadow-md ${
            readyRecall
              ? 'bg-[#D97706] border-[#FFFBEB] text-amber-100 hover:bg-[#b45309]'
              : 'bg-[#292524] border-[#78716C] text-stone-500 cursor-not-allowed opacity-60'
          }`}
        >
          <span className="text-xs font-black tracking-wide font-dwarven">
            {game.recall > 0 ? `RICHIAMO (${Math.ceil(game.recall)}s)` : 'RICHIAMA SFERA'}
          </span>
          <span className="text-[10px] opacity-80">Salva sullo Scudo</span>
        </button>
      </div>
    );
  }

  if (game.phase === 'build') {
    const wallType = isCrystal ? 'crystal_wall' : isForest ? 'wood_wall' : 'wall';
    const isPlacingWall = placingType === wallType;
    const canWall = game.gold >= GameEngine.COST_WALL;

    const turretType = isCrystal ? 'laser_turret' : 'turret';
    const isPlacingTurret = placingType === turretType;
    const turretCost = isCrystal ? 7 : isForest ? 6 : GameEngine.COST_TURRET;
    const canTurret = game.gold >= turretCost;

    return (
      <div className="absolute bottom-0 left-0 right-0 z-20 pointer-events-auto bg-gradient-to-t from-[#120A05] via-[#1E110A] to-transparent pt-3 pb-3 px-3 flex gap-2 justify-between border-t border-[#573E30]/60 select-none">
        {/* Pulsante Mura */}
        <button
          onClick={handleWallSelect}
          disabled={!canWall && !isPlacingWall}
          className={`flex-1 py-2 px-1 rounded-lg border flex flex-col items-center justify-center transition active:scale-95 shadow-md ${
            isPlacingWall
              ? 'bg-[#047857] border-white text-emerald-100 animate-pulse'
              : canWall
              ? 'bg-[#065F46] border-[#D1FAE5] text-emerald-100 hover:bg-[#047857]'
              : 'bg-[#292524] border-[#78716C] text-stone-500 cursor-not-allowed opacity-50'
          }`}
        >
          <span className="text-xs font-black tracking-wide font-dwarven">
            {isCrystal ? '+ OSSIDIANA (3G)' : isForest ? '+ BARRICATA (3G)' : '+ MURA (3G)'}
          </span>
          <span className="text-[10px] opacity-80">
            {isPlacingWall ? '▶ Tocca il campo' : isCrystal ? 'Barricata vulcanica' : isForest ? 'Tronchi appuntiti' : 'Mura in pietra'}
          </span>
        </button>

        {/* Pulsante Torretta */}
        <button
          onClick={handleTurretSelect}
          disabled={!canTurret && !isPlacingTurret}
          className={`flex-1 py-2 px-1 rounded-lg border flex flex-col items-center justify-center transition active:scale-95 shadow-md ${
            isPlacingTurret
              ? 'bg-[#0891B2] border-white text-cyan-100 animate-pulse'
              : canTurret
              ? 'bg-[#0E7490] border-[#A5F3FC] text-cyan-100 hover:bg-[#0891B2]'
              : 'bg-[#292524] border-[#78716C] text-stone-500 cursor-not-allowed opacity-50'
          }`}
        >
          <span className="text-xs font-black tracking-wide font-dwarven">
            {isCrystal ? '+ LASER (7G)' : isForest ? `+ BALISTA (${turretCost}G)` : '+ BALISTA (7G)'}
          </span>
          <span className="text-[10px] opacity-80">
            {isPlacingTurret ? '▶ Tocca il campo' : isCrystal ? 'Lente prismatica' : 'Dardi automatici'}
          </span>
        </button>

        {/* Pulsante Inizia Ondata */}
        <button
          onClick={handleStartWave}
          className="flex-1 py-2 px-1 rounded-lg border border-[#FFFBEB] bg-[#B91C1C] text-amber-50 hover:bg-[#dc2626] flex flex-col items-center justify-center transition active:scale-95 shadow-md"
        >
          <span className="text-xs font-black tracking-wide font-dwarven">
            ALLARME ➔
          </span>
          <span className="text-[10px] text-amber-200">Affronta l'ondata</span>
        </button>
      </div>
    );
  }

  return null;
};
