import { useState } from 'react';
import { Package, ArrowRightLeft, ClipboardCheck } from 'lucide-react';
import { Card, CardContent } from './ui/card';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Tabs, TabsList, TabsTrigger, TabsContent } from './ui/tabs';
import { Variant } from './VariantsTab';

interface WarehouseStock {
  variantId: string;
  stockPairs: string;
  stockBoxes: string;
  stockReserved: string;
  location: string;
  lastUpdate: string;
}

interface InventoryTabProps {
  variants: Variant[];
}

const WAREHOUSES = [
  { id: 'principal', name: 'Bodega Principal' },
  { id: 'sucursal-1', name: 'Sucursal 1' },
  { id: 'sucursal-2', name: 'Sucursal 2' }
];

export function InventoryTab({ variants }: InventoryTabProps) {
  const [selectedWarehouse, setSelectedWarehouse] = useState('principal');
  const [warehouseStocks, setWarehouseStocks] = useState<Record<string, WarehouseStock[]>>({
    'principal': variants.map(v => ({
      variantId: v.id,
      stockPairs: '24',
      stockBoxes: '2',
      stockReserved: '3',
      location: 'A-1',
      lastUpdate: new Date().toLocaleString()
    })),
    'sucursal-1': variants.map(v => ({
      variantId: v.id,
      stockPairs: '12',
      stockBoxes: '1',
      stockReserved: '1',
      location: 'B-2',
      lastUpdate: new Date().toLocaleString()
    })),
    'sucursal-2': variants.map(v => ({
      variantId: v.id,
      stockPairs: '18',
      stockBoxes: '1',
      stockReserved: '2',
      location: 'C-3',
      lastUpdate: new Date().toLocaleString()
    }))
  });

  const updateStock = (warehouse: string, variantId: string, field: keyof WarehouseStock, value: string) => {
    setWarehouseStocks(prev => ({
      ...prev,
      [warehouse]: prev[warehouse]?.map(stock => 
        stock.variantId === variantId 
          ? { ...stock, [field]: value, lastUpdate: new Date().toLocaleString() }
          : stock
      ) || []
    }));
  };

  const currentStocks = warehouseStocks[selectedWarehouse] || [];

  return (
    <Card className="border-0 shadow-md">
      <CardContent className="p-6">
        {/* Selector de Bodega */}
        <Tabs value={selectedWarehouse} onValueChange={setSelectedWarehouse} className="mb-6">
          <TabsList className="grid w-full grid-cols-3">
            {WAREHOUSES.map(warehouse => (
              <TabsTrigger key={warehouse.id} value={warehouse.id}>
                {warehouse.name}
              </TabsTrigger>
            ))}
          </TabsList>
        </Tabs>

        {/* Acciones Rápidas */}
        <div className="flex gap-3 mb-6">
          <Button variant="outline" style={{ borderColor: '#3498DB', color: '#3498DB' }}>
            <Package className="w-4 h-4 mr-2" />
            Ajuste Manual
          </Button>
          <Button variant="outline" style={{ borderColor: '#3498DB', color: '#3498DB' }}>
            <ArrowRightLeft className="w-4 h-4 mr-2" />
            Traspaso
          </Button>
          <Button variant="outline" style={{ borderColor: '#3498DB', color: '#3498DB' }}>
            <ClipboardCheck className="w-4 h-4 mr-2" />
            Conteo
          </Button>
        </div>

        {/* Tabla de Inventario */}
        <div className="overflow-x-auto border rounded-lg">
          <table className="w-full text-sm">
            <thead style={{ backgroundColor: '#ECF0F1' }}>
              <tr>
                <th className="px-4 py-3 text-left font-medium text-gray-700">Variante</th>
                <th className="px-4 py-3 text-left font-medium text-gray-700">Stock Pares</th>
                <th className="px-4 py-3 text-left font-medium text-gray-700">Stock Cajas</th>
                <th className="px-4 py-3 text-left font-medium text-gray-700">Stock Reservado</th>
                <th className="px-4 py-3 text-left font-medium text-gray-700">Ubicación</th>
                <th className="px-4 py-3 text-left font-medium text-gray-700">Última Actualización</th>
              </tr>
            </thead>
            <tbody>
              {variants.map((variant, index) => {
                const stock = currentStocks.find(s => s.variantId === variant.id);
                if (!stock) return null;

                return (
                  <tr 
                    key={variant.id}
                    className={`border-t hover:bg-gray-50 transition-colors ${
                      index % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'
                    }`}
                  >
                    {/* Variante */}
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <div 
                          className="w-6 h-6 rounded-full border-2 border-gray-300"
                          style={{ backgroundColor: variant.colorHex }}
                        />
                        <div>
                          <div className="font-medium">
                            Talla {variant.size} - {variant.color}
                          </div>
                          <div className="text-xs text-gray-500">{variant.sku}</div>
                        </div>
                      </div>
                    </td>

                    {/* Stock Pares */}
                    <td className="px-4 py-3">
                      <Input
                        type="number"
                        value={stock.stockPairs}
                        onChange={(e) => updateStock(selectedWarehouse, variant.id, 'stockPairs', e.target.value)}
                        className="h-9 w-24"
                      />
                    </td>

                    {/* Stock Cajas */}
                    <td className="px-4 py-3">
                      <Input
                        type="number"
                        value={stock.stockBoxes}
                        onChange={(e) => updateStock(selectedWarehouse, variant.id, 'stockBoxes', e.target.value)}
                        className="h-9 w-24"
                      />
                    </td>

                    {/* Stock Reservado */}
                    <td className="px-4 py-3">
                      <div className="px-3 py-1.5 bg-orange-100 text-orange-700 rounded font-medium inline-block">
                        {stock.stockReserved}
                      </div>
                    </td>

                    {/* Ubicación */}
                    <td className="px-4 py-3">
                      <Input
                        value={stock.location}
                        onChange={(e) => updateStock(selectedWarehouse, variant.id, 'location', e.target.value)}
                        className="h-9 w-28"
                        placeholder="Ej: A-3"
                      />
                    </td>

                    {/* Última Actualización */}
                    <td className="px-4 py-3">
                      <div className="text-xs text-gray-500">
                        {stock.lastUpdate}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {/* Resumen */}
        <div className="mt-6 p-4 bg-blue-50 rounded-lg border border-blue-200">
          <div className="grid grid-cols-3 gap-4 text-center">
            <div>
              <div className="text-sm text-gray-600">Total Pares</div>
              <div className="text-2xl font-bold" style={{ color: '#3498DB' }}>
                {currentStocks.reduce((sum, s) => sum + parseInt(s.stockPairs || '0'), 0)}
              </div>
            </div>
            <div>
              <div className="text-sm text-gray-600">Total Cajas</div>
              <div className="text-2xl font-bold" style={{ color: '#3498DB' }}>
                {currentStocks.reduce((sum, s) => sum + parseInt(s.stockBoxes || '0'), 0)}
              </div>
            </div>
            <div>
              <div className="text-sm text-gray-600">Total Reservado</div>
              <div className="text-2xl font-bold text-orange-600">
                {currentStocks.reduce((sum, s) => sum + parseInt(s.stockReserved || '0'), 0)}
              </div>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
