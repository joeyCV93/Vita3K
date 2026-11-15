// Vita3K emulator project
// Copyright (C) 2026 Vita3K team
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

#pragma once

#include <ctrl/state.h>
#include <emuenv/state.h>
#include <input/physical_key.h>

#include <array>
#include <cstdint>
#include <functional>
#include <string>

// Hotkey chords are a bitmask of held controller buttons: bit N is SDL_GamepadButton N,
// plus two virtual bits for the analog triggers.
constexpr uint32_t CTRL_CHORD_LEFT_TRIGGER = 1u << 28;
constexpr uint32_t CTRL_CHORD_RIGHT_TRIGGER = 1u << 29;

// Buttons currently held across all connected controllers (see the chord bit layout above).
uint32_t ctrl_get_held_chord(EmuEnvState &emuenv);
// Human readable form, e.g. "L1 + R1 + Start".
std::string ctrl_chord_to_string(uint32_t chord);
int ctrl_chord_button_count(uint32_t chord);

std::array<ControllerBinding, 15> get_controller_bindings_ext(EmuEnvState &emuenv);
SceCtrlExternalInputMode get_type_of_controller(CtrlState &state, int port);
// Reads the pad the way sceCtrlPeekBufferPositive does, without a guest call.
SceUInt32 ctrl_peek_buttons(EmuEnvState &emuenv, int port);
int ctrl_get(const SceUID thread_id, EmuEnvState &emuenv, int port, SceCtrlData2 *pData, SceUInt32 count, bool negative, bool is_peek, bool is_v2, bool from_ext);
void refresh_controllers(CtrlState &state, EmuEnvState &emuenv);
void update_keyboard_state(EmuEnvState &emuenv, const std::function<bool(input::PhysicalKeyCode)> &key_held);
