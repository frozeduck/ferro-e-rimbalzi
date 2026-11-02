import React from 'react';
import { GameEngine } from '../game/GameEngine';
import { AudioHelper } from '../audio/AudioSynthesizer';
import { Volume2, VolumeX, HelpCircle, Pause, Heart } from 'lucide-react';

interface GameHeaderProps {
  game: GameEngine;
  onHelp: () => void;
  onPause: () => void;
  onSoundToggle: () => void;
  soundEnabled: boolean;
}

export const GameHeader: React.FC<GameHeaderProps> = ({
  game,
  onHelp,
  onPause,
  onSoundToggle,
  soundEnabled,
}) => {
  const isCrystal = game.selectedBiome === 'crystal_cave';
  const isForest = game.selectedBiome === 'forest_wood';
  const sec = Math.max(0, Math.ceil(game.duration - game.time));

  return (
    <div className="absolute top-0 left-0 right-0 z-20 pointer-events-auto bg-gradient-to-b from-[#24150F] to-[#140C09] border-b-2 border-[#78350F] px-2 py-2 flex items-center justify-between gap-1 shadow-lg select-none">
      {/* 1. Scheda Assedio */}
      <div className="bg-[#2E190E] border border-[#B45309] rounded px-2 py-1 text-center min-w-[68px]">
        <div className="text-[10px] text-stone-300 font-bold uppercase tracking-wider">Assedio</div>
        <div className="text-sm font-black text-amber-400 font-arcade">
          {game.wave} / {game.maxWaves}
        </div>
      </div>

      {/* 2. Scheda Rocca (Cuori) */}
      <div className="bg-[#26120D] border border-red-900 rounded px-2 py-1 text-center flex-1">
        <div className="text-[10px] text-red-300 font-bold uppercase tracking-wider flex items-center justify-center gap-1">
          Rocca: {game.hp}/{game.maxHp}
        </div>
        <div className="flex items-center justify-center gap-1 mt-0.5">
          {Array.from({ length: game.maxHp }).map((_, i) => (
            <Heart
              key={i}
              size={14}
              className={`${
                i < game.hp
                  ? 'text-red-500 fill-red-500 drop-shadow-[0_0_4px_rgba(239,68,68,0.7)]'
                  : 'text-stone-700 fill-stone-800'
              }`}
            />
          ))}
        </div>
      </div>

      {/* 3. Risorse & Quota */}
      <div className="bg-[#2E190E] border border-[#B45309] rounded px-2 py-1 min-w-[90px]">
        <div className="text-xs font-bold text-amber-400 flex items-center gap-1">
          <span>Oro:</span>
          <span className="font-arcade text-sm text-yellow-300">{game.gold} G</span>
        </div>
        <div className={`text-xs font-bold ${isCrystal ? 'text-cyan-300' : isForest ? 'text-emerald-300' : 'text-emerald-400'}`}>
          {isCrystal ? 'Gemme:' : isForest ? 'Legno:' : 'Fe:'}{' '}
          <span className="font-arcade text-sm">
            {game.delivered}/{game.target}
          </span>
        </div>
      </div>

      {/* 4. Tempo */}
      <div
        className={`bg-[#26120D] border rounded px-2 py-1 text-center min-w-[50px] ${
          sec <= 5 ? 'border-red-600 animate-pulse text-red-400' : 'border-[#B45309] text-amber-200'
        }`}
      >
        <div className="text-[9px] text-stone-300 uppercase tracking-wider">Tempo</div>
        <div className="text-sm font-arcade font-bold">{sec}s</div>
      </div>

      {/* 5. Tasti Azione Rapida */}
      <div className="flex items-center gap-1">
        <button
          onClick={onSoundToggle}
          title="Audio ON/OFF"
          className="p-1.5 rounded bg-[#382214] border border-[#D97706] text-amber-100 hover:bg-[#4d2f1c] active:scale-95 transition"
        >
          {soundEnabled ? <Volume2 size={16} /> : <VolumeX size={16} className="text-stone-400" />}
        </button>
        <button
          onClick={onHelp}
          title="Guida al Gioco"
          className="p-1.5 rounded bg-[#382214] border border-[#D97706] text-amber-100 hover:bg-[#4d2f1c] active:scale-95 transition"
        >
          <HelpCircle size={16} />
        </button>
        <button
          onClick={onPause}
          title="Pausa"
          className="p-1.5 rounded bg-[#382214] border border-[#D97706] text-amber-100 hover:bg-[#4d2f1c] active:scale-95 transition"
        >
          <Pause size={16} />
        </button>
      </div>
    </div>
  );
};
