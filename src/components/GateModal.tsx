import React from 'react';
import { GameEngine } from '../game/GameEngine';
import { AudioHelper } from '../audio/AudioSynthesizer';
import { BiomeType } from '../types/game';
import { Compass, Lock, CheckCircle2, X } from 'lucide-react';

interface GateModalProps {
  game: GameEngine;
  onClose: () => void;
  onStartExpedition: (biome: BiomeType) => void;
  onUpdate: () => void;
}

export const GateModal: React.FC<GateModalProps> = ({
  game,
  onClose,
  onStartExpedition,
  onUpdate,
}) => {
  const handleSelectBiome = (biome: BiomeType) => {
    game.selectedBiome = biome;
    AudioHelper.playButtonClick();
    onUpdate();
  };

  const handleUnlockForest = () => {
    if (game.unlockForest()) {
      AudioHelper.playWarhorn();
      game.selectedBiome = 'forest_wood';
      onUpdate();
    }
  };

  const handleUnlockCrystal = () => {
    if (game.unlockCrystal()) {
      AudioHelper.playWarhorn();
      game.selectedBiome = 'crystal_cave';
      onUpdate();
    }
  };

  const isForestCanUnlock = game.clanIron >= 14;
  const isCrystalCanUnlock = game.clanIron >= 18 && game.clanWood >= 14;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
      <div className="relative w-full max-w-md bg-[#1B110B] border-2 border-[#D97706] rounded-xl p-5 shadow-2xl flex flex-col gap-4 text-amber-100 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex justify-between items-start border-b border-[#78350F] pb-3">
          <div>
            <h2 className="text-xl font-black text-amber-400 font-dwarven tracking-wide flex items-center gap-2">
              <Compass className="text-emerald-400" size={24} />
              PORTALE DELLE GALLERIE
            </h2>
            <p className="text-xs text-stone-300">
              Seleziona la rotta di scavo prima di scendere sui binari
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded text-stone-400 hover:text-white hover:bg-stone-800 transition"
          >
            <X size={20} />
          </button>
        </div>

        {/* 3 Biomes */}
        <div className="flex flex-col gap-3">
          {/* 1. Miniera di Ferro */}
          <div
            onClick={() => handleSelectBiome('mine_iron')}
            className={`p-3 rounded-lg border flex items-center gap-3 cursor-pointer transition active:scale-[0.98] ${
              game.selectedBiome === 'mine_iron'
                ? 'bg-[#351C12] border-amber-400 ring-2 ring-amber-500/50'
                : 'bg-[#20130C] border-[#78350F] hover:bg-[#2b1910]'
            }`}
          >
            <img src="/assets/sprite_miniera.png" alt="Miniera" className="w-16 h-12 object-contain pixel-art rounded" />
            <div className="flex-1">
              <div className="text-sm font-bold text-amber-300 font-dwarven flex items-center justify-between">
                <span>MINIERA DI FERRO</span>
                {game.selectedBiome === 'mine_iron' && <CheckCircle2 size={16} className="text-emerald-400" />}
              </div>
              <div className="text-[11px] text-amber-100">Gimmick: Fucina fusa, sfera pesante</div>
              <div className="text-[11px] text-stone-300">Spacca la roccia e forgia lingotti.</div>
              <div className="text-[11px] text-emerald-400 font-bold mt-0.5">
                {game.selectedBiome === 'mine_iron' ? '● SELEZIONATA' : 'Tocca per selezionare'}
              </div>
            </div>
          </div>

          {/* 2. Bosco dei Nani */}
          {game.forestUnlocked ? (
            <div
              onClick={() => handleSelectBiome('forest_wood')}
              className={`p-3 rounded-lg border flex items-center gap-3 cursor-pointer transition active:scale-[0.98] ${
                game.selectedBiome === 'forest_wood'
                  ? 'bg-[#14291D] border-emerald-400 ring-2 ring-emerald-500/50'
                  : 'bg-[#171717] border-emerald-800 hover:bg-[#1a2e21]'
              }`}
            >
              <img src="/assets/sprite_albero.png" alt="Bosco" className="w-16 h-12 object-contain pixel-art rounded" />
              <div className="flex-1">
                <div className="text-sm font-bold text-emerald-300 font-dwarven flex items-center justify-between">
                  <span>BOSCO DEI NANI</span>
                  {game.selectedBiome === 'forest_wood' && <CheckCircle2 size={16} className="text-emerald-400" />}
                </div>
                <div className="text-[11px] text-emerald-100">Gimmick: Ceppi elastici & segheria</div>
                <div className="text-[11px] text-stone-300">Raccogli legname e respingi i ragni.</div>
                <div className="text-[11px] text-emerald-400 font-bold mt-0.5">
                  {game.selectedBiome === 'forest_wood' ? '● SELEZIONATA' : 'Tocca per selezionare'}
                </div>
              </div>
            </div>
          ) : (
            <div className="p-3 rounded-lg border border-stone-700 bg-[#171717] flex items-center gap-3">
              <div className="w-16 h-12 bg-stone-900 rounded flex items-center justify-center text-stone-500">
                <Lock size={24} />
              </div>
              <div className="flex-1">
                <div className="text-sm font-bold text-stone-400 font-dwarven">BOSCO DEI NANI (BLOCCATO)</div>
                <div className="text-[11px] text-stone-400">Richiede 14 Ferro nelle riserve del Clan.</div>
                <button
                  onClick={handleUnlockForest}
                  disabled={!isForestCanUnlock}
                  className={`mt-1.5 text-xs font-bold py-1 px-3 rounded border transition ${
                    isForestCanUnlock
                      ? 'bg-amber-600 border-amber-300 text-amber-50 hover:bg-amber-500 active:scale-95'
                      : 'bg-stone-800 border-stone-700 text-stone-500 cursor-not-allowed'
                  }`}
                >
                  {isForestCanUnlock ? 'SBLOCCA BOSCO (14 Fe)' : `Servono 14 Fe (Hai: ${game.clanIron})`}
                </button>
              </div>
            </div>
          )}

          {/* 3. Caverna dei Cristalli */}
          {game.crystalUnlocked ? (
            <div
              onClick={() => handleSelectBiome('crystal_cave')}
              className={`p-3 rounded-lg border flex items-center gap-3 cursor-pointer transition active:scale-[0.98] ${
                game.selectedBiome === 'crystal_cave'
                  ? 'bg-[#1E1430] border-cyan-400 ring-2 ring-cyan-500/50'
                  : 'bg-[#171717] border-cyan-800 hover:bg-[#201833]'
              }`}
            >
              <img src="/assets/sprite_prisma.png" alt="Cristalli" className="w-16 h-12 object-contain pixel-art rounded" />
              <div className="flex-1">
                <div className="text-sm font-bold text-cyan-300 font-dwarven flex items-center justify-between">
                  <span>CAVERNA DEI CRISTALLI</span>
                  {game.selectedBiome === 'crystal_cave' && <CheckCircle2 size={16} className="text-cyan-400" />}
                </div>
                <div className="text-[11px] text-cyan-100">Gimmick: Prismi Risonanti & Multiball</div>
                <div className="text-[11px] text-stone-300">Estrai cristalli puri e taglia gemme.</div>
                <div className="text-[11px] text-cyan-400 font-bold mt-0.5">
                  {game.selectedBiome === 'crystal_cave' ? '● SELEZIONATA' : 'Tocca per selezionare'}
                </div>
              </div>
            </div>
          ) : (
            <div className="p-3 rounded-lg border border-stone-700 bg-[#171717] flex items-center gap-3">
              <div className="w-16 h-12 bg-stone-900 rounded flex items-center justify-center text-stone-500">
                <Lock size={24} />
              </div>
              <div className="flex-1">
                <div className="text-sm font-bold text-stone-400 font-dwarven">CAVERNA DEI CRISTALLI (BLOCCATA)</div>
                <div className="text-[11px] text-stone-400">Richiede 18 Fe + 14 Legname del Clan.</div>
                <button
                  onClick={handleUnlockCrystal}
                  disabled={!isCrystalCanUnlock}
                  className={`mt-1.5 text-xs font-bold py-1 px-3 rounded border transition ${
                    isCrystalCanUnlock
                      ? 'bg-cyan-600 border-cyan-300 text-cyan-50 hover:bg-cyan-500 active:scale-95'
                      : 'bg-stone-800 border-stone-700 text-stone-500 cursor-not-allowed'
                  }`}
                >
                  {isCrystalCanUnlock ? 'SBLOCCA CRISTALLI (18 Fe + 14 Legno)' : `Servono 18 Fe + 14 Legno`}
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Start expedition button */}
        <button
          onClick={() => onStartExpedition(game.selectedBiome)}
          className="w-full py-3 rounded-xl bg-gradient-to-r from-emerald-600 to-emerald-500 border-2 border-emerald-300 text-white font-black text-base shadow-lg hover:from-emerald-500 hover:to-emerald-400 active:scale-[0.98] transition font-dwarven tracking-wide"
        >
          {game.selectedBiome === 'mine_iron'
            ? 'ENTRA NELLA MINIERA ➔'
            : game.selectedBiome === 'forest_wood'
            ? 'ESPLORA IL BOSCO ➔'
            : 'DISCENDI NEI CRISTALLI ➔'}
        </button>

        {/* Chiudi */}
        <button
          onClick={onClose}
          className="w-full py-2.5 rounded-lg bg-[#292524] border border-[#D97706] text-amber-200 font-bold hover:bg-[#3E3835] transition"
        >
          TORNA ALLA PIAZZA
        </button>
      </div>
    </div>
  );
};
