// Bake the preview's handedness into the wearable mesh. Positive-size cubes
// preserve face culling and lighting; a negative renderer scale would not.
module.exports = function wornModel(source) {
  const model = structuredClone(source);
  const opposite = side => ({east: 'west', west: 'east'}[side] || side);
  const reflect = x => Math.round((16 - x) * 1000) / 1000;
  for (const element of model.elements) {
    const left = element.from[0];
    element.from[0] = reflect(element.to[0]);
    element.to[0] = reflect(left);
    if (element.rotation) {
      element.rotation.origin[0] = reflect(element.rotation.origin[0]);
      if (element.rotation.axis !== 'x') element.rotation.angle *= -1;
    }
    element.faces = Object.fromEntries(Object.entries(element.faces).map(([side, face]) => {
      const [u0, v0, u1, v1] = face.uv;
      face.uv = [u1, v0, u0, v1];
      if (face.rotation) face.rotation = (360 - face.rotation) % 360;
      if (face.cullface) face.cullface = opposite(face.cullface);
      return [opposite(side), face];
    }));
  }
  return model;
};
