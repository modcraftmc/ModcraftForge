/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2018-2019 DaPorkchop_ and contributors
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package fr.modcraftmc.forge.spigot;

import fr.modcraftmc.forge.config.ModcraftConfig;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.entity.item.PaintingEntity;
import net.minecraft.entity.monster.GhastEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.AmbientEntity;
import net.minecraft.entity.player.PlayerEntity;

public class TrackingRange {

    /**
     * Gets the range an entity should be 'tracked' by players and visible in
     * the client.
     *
     * @param entity
     * @return
     */
    public static int getEntityTrackingRange(Entity entity, int defaultRange) {
        if (entity instanceof PlayerEntity) {
            return ModcraftConfig.getPlayerTrackingRange();
        } else if (entity instanceof IMob) {
            return ModcraftConfig.getMonsterTrackingRange();
        } else if (entity instanceof GhastEntity) {
            return Math.max(ModcraftConfig.getMonsterActivationRange(), ModcraftConfig.getMonsterTrackingRange());
        } else if (entity instanceof CreatureEntity || entity instanceof AmbientEntity) {
            return ModcraftConfig.getAnimalTrackingRange();
        } else if (entity instanceof ItemFrameEntity || entity instanceof PaintingEntity || entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity) {
            return ModcraftConfig.getMiscTrackingRange();
        } else {
            return ModcraftConfig.getOtherTrackingRange();
        }
    }
}