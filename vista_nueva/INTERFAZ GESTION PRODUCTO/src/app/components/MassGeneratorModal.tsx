import { useState } from 'react';
import { X } from 'lucide-react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from './ui/dialog';
import { Button } from './ui/button';
import { Checkbox } from './ui/checkbox';
import { Label } from './ui/label';
import { Input } from './ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';

interface MassGeneratorModalProps {
  open: boolean;
  onClose: () => void;
  onGenerate: (config: MassGeneratorConfig) => void;
}

export interface MassGeneratorConfig {
  sizes: string[];
  colors: string[];
  basePurchasePrice: string;
  baseSalePrice: string;
  supplier: string;
  minStock: string;
}

const SIZES = [
  '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45'
];

const COLORS = [
  { id: 'negro', name: 'Negro', hex: '#000000' },
  { id: 'blanco', name: 'Blanco', hex: '#FFFFFF' },
  { id: 'rojo', name: 'Rojo', hex: '#DC2626' },
  { id: 'azul', name: 'Azul', hex: '#2563EB' },
  { id: 'verde', name: 'Verde', hex: '#16A34A' },
  { id: 'gris', name: 'Gris', hex: '#6B7280' },
  { id: 'amarillo', name: 'Amarillo', hex: '#EAB308' },
  { id: 'naranja', name: 'Naranja', hex: '#EA580C' },
  { id: 'rosa', name: 'Rosa', hex: '#EC4899' },
  { id: 'marron', name: 'Marrón', hex: '#92400E' },
];

export function MassGeneratorModal({ open, onClose, onGenerate }: MassGeneratorModalProps) {
  const [selectedSizes, setSelectedSizes] = useState<string[]>([]);
  const [selectedColors, setSelectedColors] = useState<string[]>([]);
  const [basePurchasePrice, setBasePurchasePrice] = useState('');
  const [baseSalePrice, setBaseSalePrice] = useState('');
  const [supplier, setSupplier] = useState('');
  const [minStock, setMinStock] = useState('5');

  const toggleSize = (size: string) => {
    setSelectedSizes(prev => 
      prev.includes(size) ? prev.filter(s => s !== size) : [...prev, size]
    );
  };

  const toggleColor = (colorId: string) => {
    setSelectedColors(prev => 
      prev.includes(colorId) ? prev.filter(c => c !== colorId) : [...prev, colorId]
    );
  };

  const totalVariants = selectedSizes.length * selectedColors.length;

  const handleGenerate = () => {
    onGenerate({
      sizes: selectedSizes,
      colors: selectedColors,
      basePurchasePrice,
      baseSalePrice,
      supplier,
      minStock
    });
    // Reset
    setSelectedSizes([]);
    setSelectedColors([]);
    setBasePurchasePrice('');
    setBaseSalePrice('');
    setSupplier('');
    setMinStock('5');
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-[800px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-xl font-semibold">Generador Masivo de Variantes</DialogTitle>
        </DialogHeader>

        <div className="space-y-6 py-4">
          {/* Sección 1: Tallas */}
          <div>
            <h3 className="font-medium mb-3">Seleccionar Tallas</h3>
            <div className="grid grid-cols-4 gap-3">
              {SIZES.map((size) => (
                <div key={size} className="flex items-center space-x-2">
                  <Checkbox
                    id={`size-${size}`}
                    checked={selectedSizes.includes(size)}
                    onCheckedChange={() => toggleSize(size)}
                  />
                  <Label htmlFor={`size-${size}`} className="cursor-pointer font-normal">
                    Talla {size}
                  </Label>
                </div>
              ))}
            </div>
          </div>

          {/* Sección 2: Colores */}
          <div>
            <h3 className="font-medium mb-3">Seleccionar Colores</h3>
            <div className="grid grid-cols-2 gap-3">
              {COLORS.map((color) => (
                <div 
                  key={color.id}
                  onClick={() => toggleColor(color.id)}
                  className={`p-3 border-2 rounded-lg cursor-pointer transition-all ${
                    selectedColors.includes(color.id) 
                      ? 'border-blue-500 bg-blue-50' 
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div 
                      className="w-8 h-8 rounded-full border-2 border-gray-300"
                      style={{ backgroundColor: color.hex }}
                    />
                    <span className="font-medium">{color.name}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Sección 3: Valores Base */}
          <div className="border-t pt-6">
            <h3 className="font-medium mb-4">Aplicar Valores Base a Todas las Variantes</h3>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="mass-purchase-price" className="text-sm font-medium">
                  Precio Compra Base
                </Label>
                <div className="relative mt-1.5">
                  <span className="absolute left-3 top-2.5 text-gray-500">$</span>
                  <Input
                    id="mass-purchase-price"
                    type="number"
                    value={basePurchasePrice}
                    onChange={(e) => setBasePurchasePrice(e.target.value)}
                    placeholder="0.00"
                    className="pl-7"
                  />
                </div>
              </div>

              <div>
                <Label htmlFor="mass-sale-price" className="text-sm font-medium">
                  Precio Venta Base
                </Label>
                <div className="relative mt-1.5">
                  <span className="absolute left-3 top-2.5 text-gray-500">$</span>
                  <Input
                    id="mass-sale-price"
                    type="number"
                    value={baseSalePrice}
                    onChange={(e) => setBaseSalePrice(e.target.value)}
                    placeholder="0.00"
                    className="pl-7"
                  />
                </div>
              </div>

              <div>
                <Label htmlFor="mass-supplier" className="text-sm font-medium">
                  Proveedor Común
                </Label>
                <Select value={supplier} onValueChange={setSupplier}>
                  <SelectTrigger id="mass-supplier" className="mt-1.5">
                    <SelectValue placeholder="Seleccionar proveedor" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="proveedor-1">Distribuidora SportWear S.A.</SelectItem>
                    <SelectItem value="proveedor-2">Importadora Global Shoes</SelectItem>
                    <SelectItem value="proveedor-3">Calzados Premium Ltda</SelectItem>
                    <SelectItem value="proveedor-4">Mayorista Deportes XYZ</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label htmlFor="mass-min-stock" className="text-sm font-medium">
                  Stock Mínimo Común
                </Label>
                <Input
                  id="mass-min-stock"
                  type="number"
                  value={minStock}
                  onChange={(e) => setMinStock(e.target.value)}
                  placeholder="5"
                  className="mt-1.5"
                />
              </div>
            </div>
          </div>

          {/* Preview */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p className="text-center font-medium" style={{ color: '#3498DB' }}>
              Se generarán <span className="text-2xl font-bold">{totalVariants}</span> variantes
            </p>
            <p className="text-center text-sm text-gray-600 mt-1">
              {selectedSizes.length} tallas × {selectedColors.length} colores
            </p>
          </div>

          {/* Botones */}
          <div className="flex justify-end gap-3 pt-4 border-t">
            <Button variant="outline" onClick={onClose}>
              Cancelar
            </Button>
            <Button 
              onClick={handleGenerate}
              disabled={totalVariants === 0}
              style={{ backgroundColor: '#3498DB' }}
              className="px-8"
            >
              Generar Variantes
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
