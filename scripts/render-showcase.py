"""Render the checked-in Minecraft models to shareable PNGs (no game changes)."""
import json
import math
import sys
import io
import zipfile
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/limesbackpacks'
OUT = ROOT / 'docs/showcase'
TIERS = ['leather', 'copper', 'iron', 'gold', 'diamond', 'netherite']
ACCENTS = ['#c6a081', '#d4a184', '#b7c3bb', '#c9b57c', '#d1c1a5', '#d1928b']
DETAILS = [
    'Brown leather / the beginning of the journey',
    'Warm copper / leather-fastened side pouch',
    'Soft iron / leather canteen / carry handle',
    'Muted gold / forest-green sleeping bag',
    'Soft teal / oatmeal beige bedroll / lantern',
    'Charcoal / red sleeping bag / quiver & lantern',
]
TEXTURES = {}
ARCHIVE = None
EDITION = ''


def font(size, bold=False):
    return ImageFont.truetype('C:/Windows/Fonts/' + ('segoeuib.ttf' if bold else 'segoeui.ttf'), size)


def model(tier):
    if ARCHIVE is not None:
        return json.loads(ARCHIVE.read(f'assets/limesbackpacks/models/item/{tier}_backpack.json'))
    return json.loads((ASSETS / f'models/item/{tier}_backpack.json').read_text())


def texture(identifier):
    if identifier not in TEXTURES:
        path = ASSETS / ('textures/' + identifier.split(':')[1] + '.png')
        source = io.BytesIO(ARCHIVE.read('assets/limesbackpacks/textures/' + identifier.split(':')[1] + '.png')) if ARCHIVE is not None else path
        img = Image.open(source).convert('RGBA')
        if identifier.endswith('/vanilla_lantern'):
            img = img.crop((0, 0, img.width, img.width))
        TEXTURES[identifier] = np.array(img)
    return TEXTURES[identifier]


def rotation(axis, degrees):
    a = math.radians(degrees)
    c, s = math.cos(a), math.sin(a)
    if axis == 'x':
        return np.array([[1, 0, 0], [0, c, s], [0, -s, c]])
    if axis == 'y':
        return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def mesh(m, yaw, pitch):
    camera = rotation('x', pitch) @ rotation('y', yaw)
    quads = []
    light = np.array([-.4, .8, -.7])
    light /= np.linalg.norm(light)
    for e in m['elements']:
        a, b, c = e['from']
        d, f, g = e['to']
        sides = {
            'north': [[a,f,c],[d,f,c],[d,b,c],[a,b,c]],
            'south': [[d,f,g],[a,f,g],[a,b,g],[d,b,g]],
            'east': [[d,f,c],[d,f,g],[d,b,g],[d,b,c]],
            'west': [[a,f,g],[a,f,c],[a,b,c],[a,b,g]],
            'up': [[a,f,g],[d,f,g],[d,f,c],[a,f,c]],
            'down': [[a,b,c],[d,b,c],[d,b,g],[a,b,g]],
        }
        for side, face in e['faces'].items():
            p = np.array(sides[side], dtype=float)
            if 'rotation' in e:
                r = e['rotation']
                origin = np.array(r['origin'])
                p = (p-origin) @ rotation(r['axis'], r['angle']).T + origin
            p = (p-np.array([8, 10, 8])) @ camera.T
            n = np.cross(p[1]-p[0], p[2]-p[0])
            if n[2] >= -1e-9:
                continue
            n /= np.linalg.norm(n)
            shade = .67 + .33 * max(0, np.dot(n, light))
            quads.append((p, face, shade, e['name']))
    return quads


def render(tier, width, height, yaw=-30, pitch=14):
    m = model(tier)
    quads = mesh(m, yaw, pitch)
    points = np.concatenate([q[0] for q in quads])
    low, high = points.min(axis=0), points.max(axis=0)
    scale = min(width*.87/(high[0]-low[0]), height*.89/(high[1]-low[1]))
    mid = (low+high)/2
    depth = np.full((height,width), np.inf)
    pixels = np.zeros((height,width,4), dtype=np.uint8)
    for p, face, shade, name in quads:
        tex = texture(m['textures'][face['texture'][1:]])
        u0,v0,u1,v1 = np.array(face['uv'])/16
        uv = np.array([[u0,v0],[u1,v0],[u1,v1],[u0,v1]])
        uv = np.roll(uv, int(face.get('rotation',0)/90), axis=0)
        screen = np.column_stack(((p[:,0]-mid[0])*scale+width/2,
                                  -(p[:,1]-mid[1])*scale+height/2, p[:,2]))
        for indices in ([0,1,2],[0,2,3]):
            t = screen[indices]
            tuv = uv[indices]
            x0=max(0,int(np.floor(t[:,0].min())))
            x1=min(width-1,int(np.ceil(t[:,0].max())))
            y0=max(0,int(np.floor(t[:,1].min())))
            y1=min(height-1,int(np.ceil(t[:,1].max())))
            if x1<x0 or y1<y0:
                continue
            yy,xx=np.mgrid[y0:y1+1,x0:x1+1]
            xx=xx+.5
            yy=yy+.5
            den=(t[1,1]-t[2,1])*(t[0,0]-t[2,0])+(t[2,0]-t[1,0])*(t[0,1]-t[2,1])
            if abs(den)<1e-9:
                continue
            w0=((t[1,1]-t[2,1])*(xx-t[2,0])+(t[2,0]-t[1,0])*(yy-t[2,1]))/den
            w1=((t[2,1]-t[0,1])*(xx-t[2,0])+(t[0,0]-t[2,0])*(yy-t[2,1]))/den
            w2=1-w0-w1
            z=w0*t[0,2]+w1*t[1,2]+w2*t[2,2]
            u=w0*tuv[0,0]+w1*tuv[1,0]+w2*tuv[2,0]
            v=w0*tuv[0,1]+w1*tuv[1,1]+w2*tuv[2,1]
            tx=np.clip((u*tex.shape[1]).astype(int),0,tex.shape[1]-1)
            ty=np.clip((v*tex.shape[0]).astype(int),0,tex.shape[0]-1)
            samples=tex[ty,tx]
            region=depth[y0:y1+1,x0:x1+1]
            mask=(w0>=-1e-7)&(w1>=-1e-7)&(w2>=-1e-7)&(z<region)&(samples[:,:,3]>128)
            color=samples.copy()
            color[:,:,:3]=np.clip(samples[:,:,:3].astype(float)*shade,0,255).astype(np.uint8)
            region[mask]=z[mask]
            pixels[y0:y1+1,x0:x1+1][mask]=color[mask]
    return Image.fromarray(pixels)


def background(w,h):
    # Editorial studio backdrop; the model colors themselves come from the mod.
    yy,xx=np.mgrid[:h,:w]
    glow=np.clip(1-np.sqrt(((xx-w*.5)/(w*.85))**2+((yy-h*.4)/(h*.9))**2),0,1)
    base=np.array([19,29,29])
    rgb=base+glow[:,:,None]*np.array([24,28,24])
    return Image.fromarray(rgb.astype(np.uint8)).convert('RGBA')


def shadow(img,box):
    layer=Image.new('RGBA',img.size)
    ImageDraw.Draw(layer).ellipse(box,fill=(0,0,0,95))
    img.alpha_composite(layer.filter(ImageFilter.GaussianBlur(18)))


def label(draw,xy,text,size=25,fill='#b8c1b8',bold=False):
    draw.text(xy,text,font=font(size,bold),fill=fill)


def save(img,name):
    path=OUT/name
    img.convert('RGB').save(path,optimize=True)
    print(path,flush=True)


def overview():
    img=background(2400,1900)
    d=ImageDraw.Draw(img)
    label(d,(100,65),"LIME'S BACKPACKS  /  THE COLLECTION",27,'#cab799',True)
    label(d,(95,105),'A pack for every journey.',82,'#f1e8d8',True)
    label(d,(100,220),'Six tiers. More room. More adventure.',31)
    for i,tier in enumerate(TIERS):
        x=90+(i%3)*760
        y=310+(i//3)*740
        d.rounded_rectangle((x,y,x+700,y+695),radius=20,fill=(38,48,46),outline=(70,80,70),width=2)
        label(d,(x+30,y+22),f'0{i+1}',26,ACCENTS[i],True)
        shadow(img,(x+150,y+518,x+555,y+555))
        img.alpha_composite(render(tier,650,510,-30,13),(x+25,y+40))
        d=ImageDraw.Draw(img)
        label(d,(x+32,y+550),tier.upper(),41,ACCENTS[i],True)
        description=DETAILS[i].split(' / ')
        label(d,(x+32,y+609),' / '.join(description[:2]),22)
        if len(description)>2:
            label(d,(x+32,y+643),' / '.join(description[2:]),22)
    d.line((100,1810,2300,1810),fill='#667062',width=2)
    label(d,(100,1830),'LIME'S BACKPACKS BY LIME',23,'#d5c5aa',True)
    label(d,(100,1865),'Adapted from Hiking Backpack by Flok · CC BY 4.0',18)
    label(d,(1540,1830),'Actual mod models & textures · Studio renders',23)
    save(img,'01-all-backpacks.png')


def individual(tier,i):
    img=background(1600,1600)
    d=ImageDraw.Draw(img)
    label(d,(80,58),"LIME'S BACKPACKS",26,'#d5c5aa',True)
    label(d,(1320,58),f'0{i+1} / 06',26,ACCENTS[i],True)
    label(d,(74,105),tier.upper(),106,ACCENTS[i],True)
    label(d,(82,255),DETAILS[i],27)
    shadow(img,(150,1260,970,1330))
    img.alpha_composite(render(tier,1050,950,-30,13),(10,340))
    # Complementary view shows the opposite side / canteen and rear straps.
    d=ImageDraw.Draw(img)
    d.rounded_rectangle((1100,540,1520,1205),radius=18,fill='#293531',outline='#556154',width=2)
    img.alpha_composite(render(tier,390,510,135,12),(1115,600))
    d=ImageDraw.Draw(img)
    label(d,(1130,560),'THE OTHER SIDE',21,'#b6c0b0',True)
    label(d,(1130,1145),'Crafted for the journey.',21,'#c6c8b8')
    d.line((80,1410,1520,1410),fill='#667062',width=2)
    label(d,(80,1450),'LIME'S BACKPACKS BY LIME',25,'#d5c5aa',True)
    label(d,(80,1500),'Adapted from Hiking Backpack by Flok · CC BY 4.0',20)
    label(d,(1010,1450),'Actual model & textures',23)
    save(img,f'{i+2:02d}-{tier}-backpack.png')


def both_sides(include_individuals=True):
    img=background(3000,2200)
    d=ImageDraw.Draw(img)
    label(d,(90,55),"LIME'S BACKPACKS  /  THE COMPLETE COLLECTION" + EDITION,28,'#cab799',True)
    label(d,(85,100),'Adventure from every angle.',86,'#f1e8d8',True)
    label(d,(90,215),'Six tiers. Two views each. Every detail on show.',32)
    for i,tier in enumerate(TIERS):
        x=80+(i%3)*960
        y=310+(i//3)*870
        d.rounded_rectangle((x,y,x+920,y+820),radius=20,fill='#26302e',outline='#465046',width=2)
        label(d,(x+30,y+25),f'0{i+1}  /  {tier.upper()}',37,ACCENTS[i],True)
        for j,(yaw,caption) in enumerate([(-30,'FRONT / LEFT'),(135,'REVERSE / RIGHT')]):
            px=x+10+j*450
            shadow(img,(px+75,y+640,px+365,y+675))
            img.alpha_composite(render(tier,440,590,yaw,13),(px,y+95))
            d=ImageDraw.Draw(img)
            label(d,(px+32,y+700),caption,22,'#d5c5aa',True)
        label(d,(x+30,y+766),DETAILS[i],23)
    d.line((90,2050,2910,2050),fill='#667062',width=2)
    label(d,(90,2080),'LIME'S BACKPACKS BY LIME',26,'#d5c5aa',True)
    label(d,(90,2130),'Adapted from Hiking Backpack by Flok · CC BY 4.0',21)
    label(d,(2260,2080),'Actual models & textures',25)
    save(img,'00-all-backpacks-both-sides.png')

    if not include_individuals:
        return

    # Separate full-size views are convenient for sharing or comparing details.
    for i,tier in enumerate(TIERS):
        for side,yaw in [('front-left',-30),('reverse-right',135)]:
            card=background(1200,1400)
            d=ImageDraw.Draw(card)
            label(d,(65,45),"LIME'S BACKPACKS" + EDITION,24,'#d5c5aa',True)
            label(d,(60,90),tier.upper(),78,ACCENTS[i],True)
            label(d,(65,200),side.replace('-', ' / ').upper(),24)
            shadow(card,(200,1110,1000,1160))
            card.alpha_composite(render(tier,1100,880,yaw,13),(50,265))
            d=ImageDraw.Draw(card)
            d.line((65,1210,1135,1210),fill='#667062',width=2)
            label(d,(65,1250),'LIME'S BACKPACKS BY LIME',24,'#d5c5aa',True)
            label(d,(65,1310),'Adapted from Hiking Backpack by Flok · CC BY 4.0',19)
            save(card,f'{tier}-{side}.png')


if __name__=='__main__':
    if '--monday-jar' in sys.argv:
        ARCHIVE = zipfile.ZipFile(sys.argv[sys.argv.index('--monday-jar')+1])
        OUT = ROOT / 'docs/showcase-monday'
        EDITION = '  /  MONDAY · 07 SEP 2026'
        DETAILS = [
            'Brown leather / compact pack / original hiking straps',
            'Copper-orange fabric / side pouch / reinforced feet',
            'Gray fabric / side pouches / original flask',
            'Ochre fabric / bottom-mounted sleeping bag',
            'Deep teal / original lantern / extra front pocket',
            'Dark fabric / red sleeping bag / original quiver',
        ]
    if '--output-dir' in sys.argv:
        OUT = ROOT / sys.argv[sys.argv.index('--output-dir')+1]
    OUT.mkdir(parents=True,exist_ok=True)
    if '--both-sides' in sys.argv or '--lineup-only' in sys.argv:
        both_sides(include_individuals='--lineup-only' not in sys.argv)
    else:
        overview()
        for i,tier in enumerate(TIERS):
            individual(tier,i)
