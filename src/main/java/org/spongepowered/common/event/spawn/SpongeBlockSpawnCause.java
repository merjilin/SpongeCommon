/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.event.spawn;

import com.google.common.base.Objects;
import org.spongepowered.api.event.cause.entity.spawn.common.AbstractBlockSpawnCause;

public class SpongeBlockSpawnCause extends AbstractBlockSpawnCause {

    public SpongeBlockSpawnCause(SpongeBlockSpawnCauseBuilder builder) {
        super(builder);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SpongeBlockSpawnCause that = (SpongeBlockSpawnCause) o;
        return Objects.equal(this.blockSnapshot, that.blockSnapshot) && Objects.equal(this.getType(), that.getType());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getType(), this.blockSnapshot);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper("BlockSpawnCause")
                .add("SpawnType", this.getType())
                .add("BlockSnapshot", this.blockSnapshot)
                .toString();
    }

}
