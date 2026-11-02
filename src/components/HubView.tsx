import React, { useState } from 'react';
import { GameEngine } from '../game/GameEngine';
import { AudioHelper } from '../audio/AudioSynthesizer';
import { ForgeModal } from './ForgeModal';
import { TavernModal } from './TavernModal';
import { BankModal } from './BankModal';
import { GateModal } from './GateModal';
import { BiomeType, HubModal } from '../types/game';
import { Volume2, VolumeX, Shield, Sparkles, HelpCircle } from 'lucide-react';

interface HubViewProps {
  game: GameEngine;
  onStartExpedition: (biome: BiomeType) => void;
  onOpenTutorial: () => void;
  onSoundToggle: () => void;
  soundEnabled: boolean;
}

export const HubView: React.FC<HubViewProps> = ({
  game,
  onStartExpedition,
  onOpenTutorial,
  onSoundToggle,
  soundEnabled,
}) => {
  const [hubModal, setHubModal] = useState<HubModal>('plaza');
  const [, setTick] = useState(0);

  const forceUpdate = () => setTick((t) => t + 1);

  const openModal = (modal: HubModal) => {
    AudioHelper.playButtonClick();
    setHubModal(modal);
  };

  const closeModal = () => {
    AudioHelper.playButtonClick();
    setHubModal('plaza');
    forceUpdate();
  };

  const totalUpgrades = game.clanUpgradeHp + game.clanUpgradeCapacity + game.clanUpgradeDamage;

  return (
    <div className="relative w-full h-full max-w-[540px] max-h-[960px] aspect-[9/16] bg-[#140C09] flex flex-col justify-between p-4 overflow-hidden select-none border-x border-[#382214] shadow-2xl">
      {/* Background with pixel art cavern */}
      <div
        className="absolute inset-0 bg-cover bg-center opacity-40 pointer-events-none pixel-art"
        style={{ backgroundImage: `url('/assets/sfondo_caverna.png')` }}
      />

      {/* Header */}
      <div className="relative z-10 flex flex-col items-center gap-1.5 pt-2">
        <div className="w-full flex items-center justify-between">
          <button
            onClick={onOpenTutorial}
            className="p-2 rounded-lg bg-[#26150F] border border-[#B45309] text-amber-200 hover:bg-[#3d2218] transition flex items-center gap-1 text-xs font-bold"
          >
            <HelpCircle size={16} />
            <span>Guida</span>
          </button>

          <div className="text-center">
            <h1 className="text-2xl font-black text-amber-400 font-dwarven tracking-wider drop-shadow-md">
              ROCCAFORTE DEI NANI
            </h1>
            <p className="text-[11px] text-stone-300 tracking-wide uppercase">
              Piazza dell'Avamposto Centrale
            </p>
          </div>

          <button
            onClick={onSoundToggle}
            className="p-2 rounded-lg bg-[#26150F] border border-[#B45309] text-amber-200 hover:bg-[#3d2218] transition"
            title="Audio ON/OFF"
          >
            {soundEnabled ? <Volume2 size={16} /> : <VolumeX size={16} className="text-stone-400" />}
          </button>
        </div>

        {/* Treasury Chip */}
        <div className="w-full bg-[#26150F]/90 border-2 border-[#B45309] rounded-xl py-2 px-3 flex items-center justify-around shadow-inner text-xs font-bold mt-1">
          <div className="flex items-center gap-1 text-amber-400">
            <span>Fe:</span>
            <span className="font-arcade text-base text-amber-300">{game.clanIron}</span>
          </div>
          <span className="text-stone-600">|</span>
          <div className="flex items-center gap-1 text-emerald-400">
            <span>Legno:</span>
            <span className="font-arcade text-base text-emerald-300">{game.clanWood}</span>
          </div>
          <span className="text-stone-600">|</span>
          <div className="flex items-center gap-1 text-cyan-400">
            <span>Gemme:</span>
            <span className="font-arcade text-base text-cyan-300">{game.clanGems}</span>
          </div>
          <span className="text-stone-600">|</span>
          <div className="flex items-center gap-1 text-yellow-400">
            <span>Vittorie:</span>
            <span className="font-arcade text-base text-yellow-300">{game.clanRunsWon}</span>
          </div>
        </div>
      </div>

      {/* 2x2 Interactive Buildings Grid */}
      <div className="relative z-10 grid grid-cols-2 gap-3 my-auto">
        {/* 1. Grande Fucina */}
        <div
          onClick={() => openModal('forge')}
          className="group relative bg-[#2A150D]/90 border-2 border-[#F59E0B] rounded-xl p-3 flex flex-col items-center justify-between gap-1.5 cursor-pointer shadow-lg hover:border-amber-300 hover:bg-[#381c11] active:scale-95 transition"
        >
          <div className="w-20 h-20 flex items-center justify-center p-1">
            <img
              src="/assets/sprite_hub_forgia.png"
              alt="Grande Fucina"
              className="max-w-full max-h-full object-contain pixel-art drop-shadow group-hover:scale-105 transition"
            />
          </div>
          <div className="text-center w-full">
            <div className="text-sm font-black text-amber-400 font-dwarven tracking-wide">
              GRANDE FUCINA
            </div>
            <div className="text-[11px] text-stone-300">Potenzia il Clan</div>
            <div className="text-[11px] font-bold text-emerald-400 mt-0.5">
              {totalUpgrades}/8 Migliorie
            </div>
            <div className="text-[10px] font-bold text-amber-200 mt-1 uppercase tracking-wider group-hover:text-amber-100">
              Tocca per entrare
            </div>
          </div>
        </div>

        {/* 2. Taverna della Botte */}
        <div
          onClick={() => openModal('tavern')}
          className="group relative bg-[#26180F]/90 border-2 border-[#B45309] rounded-xl p-3 flex flex-col items-center justify-between gap-1.5 cursor-pointer shadow-lg hover:border-amber-400 hover:bg-[#332014] active:scale-95 transition"
        >
          <div className="w-20 h-20 flex items-center justify-center p-1">
            <img
              src="/assets/sprite_hub_taverna.png"
              alt="Taverna"
              className="max-w-full max-h-full object-contain pixel-art drop-shadow group-hover:scale-105 transition"
            />
          </div>
          <div className="text-center w-full">
            <div className="text-sm font-black text-amber-400 font-dwarven tracking-wide">
              TAVERNA DELLA BOTTE
            </div>
            <div className="text-[11px] text-stone-300">Bevi e ricevi buff</div>
            <div className="text-[11px] font-bold mt-0.5 text-stone-400">
              {game.clanBuff ? (
                <span className="text-emerald-400 font-bold flex items-center justify-center gap-1">
                  <Sparkles size={12} /> BENEDIZIONE ATTIVA
                </span>
              ) : (
                'Nessun buff attivo'
              )}
            </div>
            <div className="text-[10px] font-bold text-amber-200 mt-1 uppercase tracking-wider group-hover:text-amber-100">
              Tocca per entrare
            </div>
          </div>
        </div>

        {/* 3. Tesoreria Reale */}
        <div
          onClick={() => openModal('bank')}
          className="group relative bg-[#1C1917]/90 border-2 border-[#78716C] rounded-xl p-3 flex flex-col items-center justify-between gap-1.5 cursor-pointer shadow-lg hover:border-stone-400 hover:bg-[#252220] active:scale-95 transition"
        >
          <div className="w-20 h-20 flex items-center justify-center p-1">
            <img
              src="/assets/sprite_hub_banca.png"
              alt="Tesoreria"
              className="max-w-full max-h-full object-contain pixel-art drop-shadow group-hover:scale-105 transition"
            />
          </div>
          <div className="text-center w-full">
            <div className="text-sm font-black text-amber-400 font-dwarven tracking-wide">
              TESORERIA REALE
            </div>
            <div className="text-[11px] text-stone-300">Bilancio & Annali</div>
            <div className="text-[11px] font-bold text-amber-400 mt-0.5">
              {game.clanTotalMined} Fe storici
            </div>
            <div className="text-[10px] font-bold text-amber-200 mt-1 uppercase tracking-wider group-hover:text-amber-100">
              Tocca per entrare
            </div>
          </div>
        </div>

        {/* 4. Portale Gallerie */}
        <div
          onClick={() => openModal('gate')}
          className="group relative bg-[#14291D]/90 border-2 border-[#059669] rounded-xl p-3 flex flex-col items-center justify-between gap-1.5 cursor-pointer shadow-lg hover:border-emerald-400 hover:bg-[#1a3526] active:scale-95 transition"
        >
          <div className="w-20 h-20 flex items-center justify-center p-1">
            <img
              src="/assets/sprite_hub_cancello.png"
              alt="Portale"
              className="max-w-full max-h-full object-contain pixel-art drop-shadow group-hover:scale-105 transition"
            />
          </div>
          <div className="text-center w-full">
            <div className="text-sm font-black text-emerald-400 font-dwarven tracking-wide">
              PORTALE GALLERIE
            </div>
            <div className="text-[11px] text-stone-300">Scendi nelle profondità</div>
            <div className="text-[11px] font-bold text-emerald-300 mt-0.5">
              {game.selectedBiome === 'mine_iron'
                ? 'Miniera pronta'
                : game.selectedBiome === 'forest_wood'
                ? 'Bosco pronto'
                : 'Cristalli pronti'}
            </div>
            <div className="text-[10px] font-bold text-emerald-300 mt-1 uppercase tracking-wider group-hover:text-emerald-100 animate-pulse">
              TOCCA PER PARTIRE ➔
            </div>
          </div>
        </div>
      </div>

      {/* Chieftain Dwarf Banner */}
      <div className="relative z-10 flex flex-col items-center text-center bg-[#1B110B]/90 border border-[#78350F] rounded-xl p-3 shadow-lg mb-2">
        <div className="flex items-center gap-3">
          <img
            src="/assets/sprite_nano.png"
            alt="Capoclan"
            className="w-14 h-14 object-contain pixel-art drop-shadow-md"
          />
          <div className="text-left">
            <div className="text-sm font-black text-amber-400 font-dwarven">
              CAPOCLAN DELLA ROCCA
            </div>
            <p className="text-xs text-stone-300">
              «Visita la Fucina o la Taverna, poi varca il Portale per estrarre risorse per il regno!»
            </p>
          </div>
        </div>
      </div>

      {/* Modals */}
      {hubModal === 'forge' && <ForgeModal game={game} onClose={closeModal} onUpdate={forceUpdate} />}
      {hubModal === 'tavern' && <TavernModal game={game} onClose={closeModal} onUpdate={forceUpdate} />}
      {hubModal === 'bank' && <BankModal game={game} onClose={closeModal} />}
      {hubModal === 'gate' && (
        <GateModal
          game={game}
          onClose={closeModal}
          onStartExpedition={(biome) => {
            closeModal();
            onStartExpedition(biome);
          }}
          onUpdate={forceUpdate}
        />
      )}
    </div>
  );
};
