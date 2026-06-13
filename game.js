const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');

canvas.width = 1200;
canvas.height = 800;

// Game state
const gameState = {
    running: true,
    paused: false,
    gameOver: false,
    roomsCleared: 0,
    enemiesDefeated: 0,
    currentLevel: 1,
    kills: 0,
    mouseX: canvas.width / 2,
    mouseY: canvas.height / 2,
    lastEnteredDoor: null
};

// Constants
const ROOM_WIDTH = 1200;
const ROOM_HEIGHT = 800;
const TILE_SIZE = 40;
const DOOR_SIZE = 30;

// Mouse tracking
canvas.addEventListener('mousemove', (e) => {
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    gameState.mouseX = (e.clientX - rect.left) * scaleX;
    gameState.mouseY = (e.clientY - rect.top) * scaleY;
});

// Input handling
const keys = {};
let isMouseDown = false;

window.addEventListener('keydown', (e) => {
    keys[e.key] = true;
    if (e.key === 'Escape') {
        gameState.paused = !gameState.paused;
    }
});

window.addEventListener('keyup', (e) => {
    keys[e.key] = false;
});

canvas.addEventListener('mousedown', () => {
    isMouseDown = true;
});

canvas.addEventListener('mouseup', () => {
    isMouseDown = false;
});

// Player class
class Player {
    constructor() {
        this.x = ROOM_WIDTH / 2 - 15;
        this.y = ROOM_HEIGHT / 2 - 15;
        this.width = 30;
        this.height = 30;
        this.speed = 1.5;
        this.maxHealth = 100;
        this.health = 100;
        this.shootCooldown = 0;
        this.invulnerable = 0;
        this.gunLength = 20;
    }

    update() {
        // Movement - ZQSD (AZERTY) instead of WASD
        let moved = false;
        if (keys['ArrowUp'] || keys['z'] || keys['Z']) {
            this.y = Math.max(10, this.y - this.speed);
            moved = true;
        }
        if (keys['ArrowDown'] || keys['s'] || keys['S']) {
            this.y = Math.min(ROOM_HEIGHT - this.height - 10, this.y + this.speed);
            moved = true;
        }
        if (keys['ArrowLeft'] || keys['q'] || keys['Q']) {
            this.x = Math.max(10, this.x - this.speed);
            moved = true;
        }
        if (keys['ArrowRight'] || keys['d'] || keys['D']) {
            this.x = Math.min(ROOM_WIDTH - this.width - 10, this.x + this.speed);
            moved = true;
        }

        // Shooting
        if (isMouseDown && this.shootCooldown <= 0) {
            this.shoot();
            this.shootCooldown = 20;
        }
        this.shootCooldown--;
        this.invulnerable--;
    }

    shoot() {
        // Calculate direction to mouse
        const dx = gameState.mouseX - (this.x + this.width / 2);
        const dy = gameState.mouseY - (this.y + this.height / 2);
        const dist = Math.sqrt(dx * dx + dy * dy);
        
        // Normalize and create bullet
        if (dist > 0) {
            const vx = (dx / dist) * 6;
            const vy = (dy / dist) * 6;
            bullets.push(new Bullet(this.x + this.width / 2, this.y + this.height / 2, vx, vy));
        }
    }

    takeDamage(amount) {
        if (this.invulnerable <= 0) {
            this.health -= amount;
            this.invulnerable = 30;
            if (this.health <= 0) {
                endGame();
            }
        }
    }

    draw() {
        // Flash when invulnerable
        if (this.invulnerable > 0 && Math.floor(this.invulnerable / 5) % 2 === 0) return;

        const centerX = this.x + this.width / 2;
        const centerY = this.y + this.height / 2;

        // Player body
        ctx.fillStyle = '#00ff88';
        ctx.beginPath();
        ctx.arc(centerX, centerY, this.width / 2, 0, Math.PI * 2);
        ctx.fill();

        // Border
        ctx.strokeStyle = '#ff00ff';
        ctx.lineWidth = 2;
        ctx.stroke();

        // Calculate angle to mouse
        const dx = gameState.mouseX - centerX;
        const dy = gameState.mouseY - centerY;
        const angle = Math.atan2(dy, dx);

        // Gun barrel
        ctx.strokeStyle = '#ffff00';
        ctx.lineWidth = 3;
        ctx.lineCap = 'round';
        ctx.beginPath();
        ctx.moveTo(centerX, centerY);
        ctx.lineTo(
            centerX + Math.cos(angle) * this.gunLength,
            centerY + Math.sin(angle) * this.gunLength
        );
        ctx.stroke();

        // Gun tip glow
        ctx.fillStyle = '#ffff00';
        ctx.beginPath();
        ctx.arc(
            centerX + Math.cos(angle) * this.gunLength,
            centerY + Math.sin(angle) * this.gunLength,
            4,
            0,
            Math.PI * 2
        );
        ctx.fill();

        // Center point
        ctx.fillStyle = '#ff00ff';
        ctx.fillRect(centerX - 3, centerY - 3, 6, 6);
    }
}

// Bullet class
class Bullet {
    constructor(x, y, vx = 0, vy = -8) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = 3;
        this.speed = 8;
    }

    update() {
        this.x += this.vx;
        this.y += this.vy;
    }

    draw() {
        ctx.fillStyle = '#00ff88';
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
        ctx.fill();

        ctx.strokeStyle = 'rgba(0, 255, 136, 0.8)';
        ctx.lineWidth = 1;
        ctx.stroke();
    }

    isOffScreen() {
        return this.x < -10 || this.x > ROOM_WIDTH + 10 || this.y < -10 || this.y > ROOM_HEIGHT + 10;
    }
}

// Door class
class Door {
    constructor(x, y, direction, isEntryDoor = false) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.width = DOOR_SIZE;
        this.height = DOOR_SIZE;
        this.isEntryDoor = isEntryDoor;
    }

    draw(roomCleared) {
        // Only show doors if room is cleared or this is the entry door
        if (!roomCleared && !this.isEntryDoor) return;

        ctx.fillStyle = this.isEntryDoor ? 'rgba(255, 100, 100, 0.3)' : 'rgba(0, 255, 136, 0.3)';
        ctx.fillRect(this.x - this.width / 2, this.y - this.height / 2, this.width, this.height);

        ctx.strokeStyle = this.isEntryDoor ? '#ff6464' : '#00ff88';
        ctx.lineWidth = 2;
        ctx.strokeRect(this.x - this.width / 2, this.y - this.height / 2, this.width, this.height);

        // Direction indicator
        ctx.fillStyle = this.isEntryDoor ? '#ff6464' : '#00ff88';
        ctx.font = 'bold 12px Courier New';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        const arrow = this.direction === 'up' ? '↑' : this.direction === 'down' ? '↓' : this.direction === 'left' ? '←' : '→';
        ctx.fillText(arrow, this.x, this.y);
    }
}

// Obstacle class
class Obstacle {
    constructor(x, y, width, height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    draw() {
        ctx.fillStyle = '#00ffff';
        ctx.fillRect(this.x, this.y, this.width, this.height);

        ctx.strokeStyle = '#0088ff';
        ctx.lineWidth = 2;
        ctx.strokeRect(this.x, this.y, this.width, this.height);

        // Crosshatch pattern
        ctx.strokeStyle = 'rgba(0, 136, 255, 0.3)';
        ctx.lineWidth = 1;
        for (let i = this.x; i < this.x + this.width; i += 10) {
            ctx.beginPath();
            ctx.moveTo(i, this.y);
            ctx.lineTo(i + this.height, this.y + this.height);
            ctx.stroke();
        }
    }

    collidesWith(x, y, radius) {
        return !(x + radius < this.x || x - radius > this.x + this.width ||
                 y + radius < this.y || y - radius > this.y + this.height);
    }
}

// Room class
class Room {
    constructor(id, difficulty = 1, entryDoor = null) {
        this.id = id;
        this.difficulty = difficulty;
        this.entryDoor = entryDoor;
        this.enemies = [];
        this.doors = [];
        this.obstacles = [];
        this.cleared = false;
        this.isStartingRoom = id === 0;
        this.generate();
    }

    generate() {
        // Spawn doors
        this.doors = [];
        const doorDirections = [];
        
        if (Math.random() < 0.7) doorDirections.push('up');
        if (Math.random() < 0.7) doorDirections.push('down');
        if (Math.random() < 0.7) doorDirections.push('left');
        if (Math.random() < 0.7) doorDirections.push('right');

        // Ensure at least one door
        if (doorDirections.length === 0) doorDirections.push('up');

        for (let dir of doorDirections) {
            let x, y;
            if (dir === 'up') { x = ROOM_WIDTH / 2; y = 20; }
            else if (dir === 'down') { x = ROOM_WIDTH / 2; y = ROOM_HEIGHT - 20; }
            else if (dir === 'left') { x = 20; y = ROOM_HEIGHT / 2; }
            else { x = ROOM_WIDTH - 20; y = ROOM_HEIGHT / 2; }
            
            const isEntry = this.entryDoor && this.entryDoor === dir;
            this.doors.push(new Door(x, y, dir, isEntry));
        }

        // Starting room has no enemies or obstacles
        if (this.isStartingRoom) {
            this.enemies = [];
            this.obstacles = [];
            this.cleared = false; // Allow doors to be shown
            return;
        }

        // Spawn obstacles randomly (not in starting room)
        this.obstacles = [];
        const obstacleCount = 3 + Math.floor(this.difficulty / 2);
        for (let i = 0; i < obstacleCount; i++) {
            let x, y, width, height, overlaps;
            
            do {
                overlaps = false;
                width = 60 + Math.random() * 80;
                height = 40 + Math.random() * 60;
                x = Math.random() * (ROOM_WIDTH - width - 100) + 50;
                y = Math.random() * (ROOM_HEIGHT - height - 100) + 50;
                
                for (let door of this.doors) {
                    if (!(x + width < door.x - 40 || x > door.x + 40 ||
                          y + height < door.y - 40 || y > door.y + 40)) {
                        overlaps = true;
                        break;
                    }
                }
                
                for (let obs of this.obstacles) {
                    if (!(x + width < obs.x || x > obs.x + obs.width ||
                          y + height < obs.y || y > obs.y + obs.height)) {
                        overlaps = true;
                        break;
                    }
                }
            } while (overlaps);
            
            this.obstacles.push(new Obstacle(x, y, width, height));
        }

        // Spawn enemies (with delayed shooting so player has time to react)
        this.enemies = [];
        const enemyCount = 2 + Math.floor(this.difficulty / 2);
        for (let i = 0; i < enemyCount; i++) {
            let x, y, overlaps;
            
            do {
                overlaps = false;
                x = Math.random() * (ROOM_WIDTH - 60) + 30;
                y = Math.random() * (ROOM_HEIGHT - 100) + 50;
                
                for (let obs of this.obstacles) {
                    if (x + 15 > obs.x && x - 15 < obs.x + obs.width &&
                        y + 15 > obs.y && y - 15 < obs.y + obs.height) {
                        overlaps = true;
                        break;
                    }
                }
            } while (overlaps);
            
            // Enemy type variety - creative mix based on difficulty
            let type = 'basic';
            const rand = Math.random();
            
            if (this.difficulty >= 4) {
                // High difficulty: mix of all types
                const types = ['basic', 'fast', 'sniper', 'heavy', 'tanky'];
                type = types[Math.floor(Math.random() * types.length)];
            } else if (this.difficulty === 3) {
                // Medium-high: fast, sniper, or heavy
                if (rand < 0.3) type = 'fast';
                else if (rand < 0.5) type = 'sniper';
                else if (rand < 0.15) type = 'heavy';
            } else if (this.difficulty === 2) {
                // Medium: some fast or heavy
                if (rand < 0.25) type = 'fast';
                else if (rand < 0.1) type = 'heavy';
            } else if (this.difficulty > 1) {
                // Early game: mostly basic with rare fast
                if (rand < 0.15) type = 'fast';
            }
            
            this.enemies.push(new Enemy(x, y, type, true)); // true = delay shooting
        }
    }

    update(player) {
        this.enemies.forEach(enemy => enemy.update(player.x, player.y));
        this.enemies = this.enemies.filter(e => e.health > 0);
        this.cleared = this.enemies.length === 0;
    }

    draw() {
        // Draw walls
        ctx.strokeStyle = '#00ff88';
        ctx.lineWidth = 3;
        ctx.strokeRect(10, 10, ROOM_WIDTH - 20, ROOM_HEIGHT - 20);

        // Draw obstacles
        this.obstacles.forEach(obs => obs.draw());

        // Draw doors
        this.doors.forEach(door => door.draw(this.cleared || this.isStartingRoom));

        // Draw enemies
        this.enemies.forEach(enemy => enemy.draw());
    }
}

// Coin class
class Coin {
    constructor(x, y) {
        this.x = x;
        this.y = y;
        this.radius = 5;
        this.vx = (Math.random() - 0.5) * 3;
        this.vy = (Math.random() - 0.5) * 3 - 1;
        this.life = 180; // Frames before disappearing
        this.collected = false;
    }

    update() {
        this.x += this.vx;
        this.y += this.vy;
        this.vy += 0.15; // Gravity
        this.life--;
        
        // Bounce off walls
        if (this.x - this.radius < 10) {
            this.x = 10 + this.radius;
            this.vx *= -0.8;
        }
        if (this.x + this.radius > ROOM_WIDTH - 10) {
            this.x = ROOM_WIDTH - 10 - this.radius;
            this.vx *= -0.8;
        }
        if (this.y - this.radius < 10) {
            this.y = 10 + this.radius;
            this.vy *= -0.8;
        }
        if (this.y + this.radius > ROOM_HEIGHT - 10) {
            this.y = ROOM_HEIGHT - 10 - this.radius;
            this.vy *= -0.8;
        }
    }

    draw() {
        if (this.life < 30) {
            ctx.globalAlpha = this.life / 30;
        }
        
        ctx.fillStyle = '#ffff00';
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
        ctx.fill();
        
        ctx.strokeStyle = '#ffaa00';
        ctx.lineWidth = 1;
        ctx.stroke();
        
        ctx.globalAlpha = 1;
    }
}

// Enemy class - variety system
class Enemy {
    constructor(x, y, type = 'basic', delayShoot = false) {
        this.x = x;
        this.y = y;
        this.width = 25;
        this.height = 25;
        this.type = type;
        this.speed = 0.7;
        this.health = 1;
        this.baseDamage = 10;
        
        // Type-specific properties
        switch(type) {
            case 'heavy':
                this.speed = 0.35;
                this.width = 38;
                this.height = 38;
                this.health = 3;
                this.baseDamage = 15;
                this.shootCooldown = delayShoot ? 200 : Math.random() * 120 + 100;
                this.color = '#ff5555';
                break;
            case 'fast':
                this.speed = 1.4;
                this.width = 18;
                this.height = 18;
                this.health = 1;
                this.baseDamage = 5;
                this.shootCooldown = delayShoot ? 90 : Math.random() * 40 + 20;
                this.color = '#ffaa00';
                break;
            case 'sniper':
                this.speed = 0.25;
                this.width = 22;
                this.height = 22;
                this.health = 1;
                this.baseDamage = 25;
                this.shootCooldown = delayShoot ? 180 : Math.random() * 200 + 150;
                this.color = '#00aaff';
                break;
            case 'tanky':
                this.speed = 0.5;
                this.width = 32;
                this.height = 32;
                this.health = 5;
                this.baseDamage = 8;
                this.shootCooldown = delayShoot ? 150 : Math.random() * 100 + 80;
                this.color = '#aa0055';
                break;
            default: // basic
                this.speed = 0.7;
                this.width = 25;
                this.height = 25;
                this.health = 1;
                this.baseDamage = 10;
                this.shootCooldown = delayShoot ? 120 : Math.random() * 80 + 60;
                this.color = '#ff00ff';
        }
    }

    update(playerX, playerY) {
        // AI: Chase player
        const dx = playerX - this.x;
        const dy = playerY - this.y;
        const dist = Math.sqrt(dx * dx + dy * dy);

        if (dist > 1) {
            this.x += (dx / dist) * this.speed;
            this.y += (dy / dist) * this.speed;
        }

        // Shooting
        this.shootCooldown--;
        if (this.shootCooldown <= 0) {
            enemyBullets.push(new EnemyBullet(this.x + this.width / 2, this.y + this.height / 2, playerX, playerY, this.baseDamage || 10));
            
            // Type-specific shoot rates
            if (this.type === 'heavy') {
                this.shootCooldown = Math.random() * 120 + 100;
            } else if (this.type === 'fast') {
                this.shootCooldown = Math.random() * 40 + 30;
            } else if (this.type === 'sniper') {
                this.shootCooldown = Math.random() * 200 + 150;
            } else {
                this.shootCooldown = Math.random() * 100 + 80;
            }
        }
    }

    draw() {
        ctx.fillStyle = this.color;
        ctx.fillRect(this.x, this.y, this.width, this.height);

        // Border
        ctx.strokeStyle = '#00ff88';
        ctx.lineWidth = 2;
        ctx.strokeRect(this.x, this.y, this.width, this.height);

        // Type indicators
        if (this.type === 'heavy') {
            // Heavy: thick red interior
            ctx.fillStyle = `rgba(255, 100, 100, 0.5)`;
            ctx.fillRect(this.x + 4, this.y + 4, this.width - 8, this.height - 8);
        } else if (this.type === 'fast') {
            // Fast: diagonal speed stripes
            ctx.strokeStyle = `rgba(255, 170, 0, 0.6)`;
            ctx.lineWidth = 2;
            for (let i = -8; i < this.width + 8; i += 5) {
                ctx.beginPath();
                ctx.moveTo(this.x + i, this.y);
                ctx.lineTo(this.x + i + this.height, this.y + this.height);
                ctx.stroke();
            }
        } else if (this.type === 'sniper') {
            // Sniper: crosshair targeting reticle
            const cx = this.x + this.width / 2;
            const cy = this.y + this.height / 2;
            ctx.strokeStyle = `rgba(0, 170, 255, 0.7)`;
            ctx.lineWidth = 1.5;
            // Crosshair
            ctx.beginPath();
            ctx.moveTo(cx - 6, cy);
            ctx.lineTo(cx + 6, cy);
            ctx.stroke();
            ctx.beginPath();
            ctx.moveTo(cx, cy - 6);
            ctx.lineTo(cx, cy + 6);
            ctx.stroke();
            // Corner dots
            ctx.fillStyle = `rgba(0, 170, 255, 0.7)`;
            const off = 4;
            ctx.beginPath();
            ctx.arc(cx - off, cy - off, 1, 0, Math.PI * 2);
            ctx.fill();
            ctx.beginPath();
            ctx.arc(cx + off, cy - off, 1, 0, Math.PI * 2);
            ctx.fill();
            ctx.beginPath();
            ctx.arc(cx - off, cy + off, 1, 0, Math.PI * 2);
            ctx.fill();
            ctx.beginPath();
            ctx.arc(cx + off, cy + off, 1, 0, Math.PI * 2);
            ctx.fill();
        } else if (this.type === 'tanky') {
            // Tanky: armor plating pattern
            ctx.fillStyle = `rgba(170, 0, 85, 0.5)`;
            for (let i = 0; i < 3; i++) {
                ctx.fillRect(this.x + 3 + i * 10, this.y + 3, 6, this.height - 6);
            }
        } else {
            // Basic: simple center square
            ctx.fillStyle = `rgba(255, 0, 255, 0.4)`;
            ctx.fillRect(this.x + 6, this.y + 6, this.width - 12, this.height - 12);
        }
    }
}

// Enemy bullet
class EnemyBullet {
    constructor(x, y, targetX, targetY, damage = 10) {
        this.x = x;
        this.y = y;
        const dx = targetX - x;
        const dy = targetY - y;
        const dist = Math.sqrt(dx * dx + dy * dy);
        this.vx = (dx / dist) * 3;
        this.vy = (dy / dist) * 3;
        this.radius = 4;
        this.damage = damage;
    }

    update() {
        this.x += this.vx;
        this.y += this.vy;
    }

    draw() {
        ctx.fillStyle = '#ff0088';
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
        ctx.fill();

        ctx.strokeStyle = 'rgba(255, 0, 136, 0.8)';
        ctx.lineWidth = 1;
        ctx.stroke();
    }

    isOffScreen() {
        return this.x < -10 || this.x > ROOM_WIDTH + 10 || this.y < -10 || this.y > ROOM_HEIGHT + 10;
    }
}

// Particle
class Particle {
    constructor(x, y, vx, vy, life = 20) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.maxLife = life;
    }

    update() {
        this.x += this.vx;
        this.y += this.vy;
        this.vy += 0.1;
        this.life--;
    }

    draw() {
        const alpha = this.life / this.maxLife;
        ctx.fillStyle = `rgba(0, 255, 136, ${alpha})`;
        ctx.beginPath();
        ctx.arc(this.x, this.y, 2, 0, Math.PI * 2);
        ctx.fill();
    }

    isDead() {
        return this.life <= 0;
    }
}

// Room manager
let currentRoom = new Room(0, 1, null);
let roomsVisited = {};
let bullets = [];
let enemyBullets = [];
let particles = [];
let coins = [];
let roomStartTime = 0; // Track when room was entered

let player = new Player();

// Collision detection
function checkCollisions() {
    // Bullet-Obstacle collisions
    for (let i = bullets.length - 1; i >= 0; i--) {
        const bullet = bullets[i];
        for (let obs of currentRoom.obstacles) {
            if (obs.collidesWith(bullet.x, bullet.y, bullet.radius)) {
                bullets.splice(i, 1);
                break;
            }
        }
    }

    // Bullet-Enemy collisions
    for (let i = bullets.length - 1; i >= 0; i--) {
        for (let j = currentRoom.enemies.length - 1; j >= 0; j--) {
            const bullet = bullets[i];
            const enemy = currentRoom.enemies[j];

            const dx = bullet.x - (enemy.x + enemy.width / 2);
            const dy = bullet.y - (enemy.y + enemy.height / 2);
            const dist = Math.sqrt(dx * dx + dy * dy);

            if (dist < bullet.radius + enemy.width / 2) {
                // Explosion
                for (let k = 0; k < 6; k++) {
                    const angle = (Math.PI * 2 * k) / 6;
                    particles.push(new Particle(
                        enemy.x + enemy.width / 2,
                        enemy.y + enemy.height / 2,
                        Math.cos(angle) * 2,
                        Math.sin(angle) * 2,
                        15
                    ));
                }

                enemy.health -= 1;
                bullets.splice(i, 1);
                gameState.kills++;
                if (enemy.health <= 0) {
                    gameState.enemiesDefeated++;
                    // Spawn coins when enemy dies
                    const coinCount = Math.random() < 0.5 ? 1 : 2;
                    for (let c = 0; c < coinCount; c++) {
                        coins.push(new Coin(enemy.x + enemy.width / 2, enemy.y + enemy.height / 2));
                    }
                }
                break;
            }
        }
    }

    // Enemy-Obstacle collisions (simple pushing)
    for (let enemy of currentRoom.enemies) {
        for (let obs of currentRoom.obstacles) {
            if (enemy.x + enemy.width / 2 > obs.x - 20 && enemy.x + enemy.width / 2 < obs.x + obs.width + 20 &&
                enemy.y + enemy.height / 2 > obs.y - 20 && enemy.y + enemy.height / 2 < obs.y + obs.height + 20) {
                // Push enemy away slightly
                const dx = (enemy.x + enemy.width / 2) - (obs.x + obs.width / 2);
                const dy = (enemy.y + enemy.height / 2) - (obs.y + obs.height / 2);
                const dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > 0) {
                    enemy.x += (dx / dist) * 0.5;
                    enemy.y += (dy / dist) * 0.5;
                }
            }
        }
    }

    // Enemy bullet-Player collisions
    for (let i = enemyBullets.length - 1; i >= 0; i--) {
        const bullet = enemyBullets[i];
        const dx = bullet.x - (player.x + player.width / 2);
        const dy = bullet.y - (player.y + player.height / 2);
        const dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < bullet.radius + player.width / 2) {
            player.takeDamage(bullet.damage || 10);
            enemyBullets.splice(i, 1);
        }
    }

    // Enemy bullet-Obstacle collisions
    for (let i = enemyBullets.length - 1; i >= 0; i--) {
        const bullet = enemyBullets[i];
        for (let obs of currentRoom.obstacles) {
            if (obs.collidesWith(bullet.x, bullet.y, bullet.radius)) {
                enemyBullets.splice(i, 1);
                break;
            }
        }
    }

    // Enemy-Player collisions
    for (let i = currentRoom.enemies.length - 1; i >= 0; i--) {
        const enemy = currentRoom.enemies[i];
        const dx = player.x - enemy.x;
        const dy = player.y - enemy.y;
        const dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < player.width / 2 + enemy.width / 2) {
            player.takeDamage(5);
        }
    }

    // Player-Obstacle collisions
    for (let obs of currentRoom.obstacles) {
        if (obs.collidesWith(player.x + player.width / 2, player.y + player.height / 2, player.width / 2)) {
            // Push player out of obstacle
            const dx = (player.x + player.width / 2) - (obs.x + obs.width / 2);
            const dy = (player.y + player.height / 2) - (obs.y + obs.height / 2);
            const dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > 0) {
                player.x += (dx / dist) * 2;
                player.y += (dy / dist) * 2;
            }
        }
    }

    // Coin collection
    for (let i = coins.length - 1; i >= 0; i--) {
        const coin = coins[i];
        const dx = player.x + player.width / 2 - coin.x;
        const dy = player.y + player.height / 2 - coin.y;
        const dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < player.width / 2 + coin.radius + 5) {
            coins.splice(i, 1);
            // Could add score increase here if desired
        }
    }

    // Door collisions
    currentRoom.doors.forEach(door => {
        const dx = player.x + player.width / 2 - door.x;
        const dy = player.y + player.height / 2 - door.y;
        const dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < player.width / 2 + door.width / 2 && currentRoom.cleared) {
            goToNextRoom(door.direction);
        }
    });
}

function goToNextRoom(direction) {
    // Calculate opposite direction for spawn
    const oppositeDir = {
        'up': 'down',
        'down': 'up',
        'left': 'right',
        'right': 'left'
    }[direction];

    // Store the door we came from (for showing it as entry door)
    gameState.lastEnteredDoor = direction;

    // Simple room progression
    gameState.roomsCleared++;
    gameState.currentLevel++;
    currentRoom = new Room(gameState.roomsCleared, gameState.currentLevel, oppositeDir);
    
    // Calculate spawn position based on opposite door
    if (oppositeDir === 'up') {
        player.x = ROOM_WIDTH / 2 - 15;
        player.y = 50;
    } else if (oppositeDir === 'down') {
        player.x = ROOM_WIDTH / 2 - 15;
        player.y = ROOM_HEIGHT - 70;
    } else if (oppositeDir === 'left') {
        player.x = 50;
        player.y = ROOM_HEIGHT / 2 - 15;
    } else {
        player.x = ROOM_WIDTH - 70;
        player.y = ROOM_HEIGHT / 2 - 15;
    }

    player.health = Math.min(player.maxHealth, player.health + 20);
    
    bullets = [];
    enemyBullets = [];
    coins = [];
    roomStartTime = 0;
}

function endGame() {
    gameState.gameOver = true;
    gameState.running = false;
    document.getElementById('gameOverScreen').classList.remove('hidden');
    document.getElementById('roomsCleared').textContent = gameState.roomsCleared;
    document.getElementById('enemiesDefeated').textContent = gameState.enemiesDefeated;
}

function updateHUD() {
    document.getElementById('health').textContent = Math.max(0, player.health);
    document.getElementById('maxHealth').textContent = player.maxHealth;
    document.getElementById('level').textContent = gameState.currentLevel;
    document.getElementById('room').textContent = gameState.roomsCleared;
    document.getElementById('kills').textContent = gameState.kills;
}

// Game loop
function gameLoop() {
    if (!gameState.paused) {
        // Update
        player.update();
        currentRoom.update(player);

        bullets.forEach(bullet => bullet.update());
        bullets = bullets.filter(bullet => !bullet.isOffScreen());

        enemyBullets.forEach(bullet => bullet.update());
        enemyBullets = enemyBullets.filter(bullet => !bullet.isOffScreen());

        particles.forEach(particle => particle.update());
        particles = particles.filter(particle => !particle.isDead());

        coins.forEach(coin => coin.update());
        coins = coins.filter(coin => coin.life > 0);

        checkCollisions();
        updateHUD();
    }

    // Draw
    ctx.fillStyle = '#0a0e27';
    ctx.fillRect(0, 0, ROOM_WIDTH, ROOM_HEIGHT);

    // Draw grid
    ctx.strokeStyle = 'rgba(0, 255, 136, 0.05)';
    ctx.lineWidth = 1;
    for (let i = 0; i < ROOM_WIDTH; i += TILE_SIZE) {
        ctx.beginPath();
        ctx.moveTo(i, 0);
        ctx.lineTo(i, ROOM_HEIGHT);
        ctx.stroke();
    }
    for (let i = 0; i < ROOM_HEIGHT; i += TILE_SIZE) {
        ctx.beginPath();
        ctx.moveTo(0, i);
        ctx.lineTo(ROOM_WIDTH, i);
        ctx.stroke();
    }

    // Draw room content
    currentRoom.draw();
    player.draw();
    bullets.forEach(bullet => bullet.draw());
    coins.forEach(coin => coin.draw());
    enemyBullets.forEach(bullet => bullet.draw());
    particles.forEach(particle => particle.draw());

    // Draw UI overlays
    if (currentRoom.cleared) {
        ctx.fillStyle = 'rgba(0, 255, 136, 0.3)';
        ctx.font = 'bold 20px Courier New';
        ctx.textAlign = 'center';
        ctx.fillText('[ ROOM CLEARED ]', ROOM_WIDTH / 2, 50);
    }

    // Draw pause screen
    if (gameState.paused) {
        ctx.fillStyle = 'rgba(10, 14, 39, 0.7)';
        ctx.fillRect(0, 0, ROOM_WIDTH, ROOM_HEIGHT);

        ctx.fillStyle = '#00ff88';
        ctx.font = 'bold 48px Courier New';
        ctx.textAlign = 'center';
        ctx.shadowColor = 'rgba(0, 255, 136, 0.8)';
        ctx.shadowBlur = 20;
        ctx.fillText('[ PAUSED ]', ROOM_WIDTH / 2, ROOM_HEIGHT / 2);
        ctx.shadowColor = 'transparent';
    }

    if (gameState.running) {
        requestAnimationFrame(gameLoop);
    }
}

// Start the game
gameLoop();
